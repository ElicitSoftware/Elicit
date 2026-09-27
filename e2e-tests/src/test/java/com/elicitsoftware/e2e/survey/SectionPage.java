package com.elicitsoftware.e2e.survey;

import com.elicitsoftware.e2e.PageObject;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;

import java.util.ArrayList;
import java.util.List;

/**
 * SectionView (Survey UC-002). Since which questions render depends on the seeded survey's
 * content and branching (not fixed structure), this fills whatever is currently visible rather
 * than targeting specific questions by id: every question component carries the
 * {@code elicit-input-field} CSS class (see {@code ElicitComponent}/{@code ElicitHtml} in the
 * Survey source), so {@link #answerAllVisibleQuestions()} queries generically by that class and
 * dispatches on tag name.
 *
 * <p>Covers the common question types (text/email/password, integer, date, combo box, radio
 * group, checkbox, multi-select combo box). Rarer types (checkbox group, date-time/time picker,
 * and the still-unimplemented {@code MODAL} placeholder -- Survey UC-002 A5) are left unanswered;
 * if one of those is required, section validation will legitimately block navigation and
 * {@link #answerSurveyUntilReview} will fail loudly rather than mis-fill it.</p>
 */
public class SectionPage extends PageObject {

    /** The chrome caption of the navigation button while a further section exists. */
    private final String nextCaption;
    /** Its caption on the last section. */
    private final String reviewCaption;

    /** A section shown in English, the language every deployment ships. */
    public SectionPage(Page page) {
        this(page, "Next", "Review");
    }

    /**
     * A section shown in a mounted language (Survey UC-009). The navigation button is always
     * {@code section-next-button}, but whether it means "next section" or "last section" can only
     * be told from its caption, which is chrome and therefore translated -- so a caller driving a
     * respondent in Spanish or Arabic must say what those two words read as there.
     *
     * @param nextCaption   {@code sectionView.btnNext} in the respondent's language
     * @param reviewCaption {@code sectionView.btnReview} in the respondent's language
     */
    public SectionPage(Page page, String nextCaption, String reviewCaption) {
        super(page);
        this.nextCaption = nextCaption;
        this.reviewCaption = reviewCaption;
    }

    /** What clicking the section's navigation button led to. */
    private enum Navigation { NEXT_SECTION, REVIEW_REACHED, REVIEW_BLOCKED }

    /** How many blocked Review clicks (validation failures on the last section) to tolerate. */
    private static final int MAX_REVIEW_ATTEMPTS = 10;

    /**
     * Repeatedly fills whatever is visible and advances until the Review page is reached.
     *
     * <p>A Review click that validation blocks (required fields the previous pass could not
     * see yet) is not an error: it simply sends the loop round again, so that the fields the
     * last answers revealed get their turn. Only {@link #MAX_REVIEW_ATTEMPTS} consecutive
     * blocked clicks -- nothing left to reveal, yet still invalid -- fail the run, with a
     * dump of the offending fields.</p>
     */
    public void answerSurveyUntilReview() {
        // FHHS-style surveys can legitimately spawn one set of sections per family member via
        // repeat-count branching (Survey UC-002 A3), so this needs real headroom, not just a
        // runaway-loop guard.
        int maxSections = 200;
        int reviewAttempts = 0;
        for (int i = 0; i < maxSections; i++) {
            answerAllVisibleQuestions();
            switch (proceedToNextSectionOrReview()) {
                case REVIEW_REACHED -> {
                    return;
                }
                case NEXT_SECTION -> reviewAttempts = 0;
                case REVIEW_BLOCKED -> {
                    if (++reviewAttempts >= MAX_REVIEW_ATTEMPTS) {
                        throw new IllegalStateException("Review stayed blocked after " + reviewAttempts
                                + " attempts on " + page.url() + "; " + invalidFieldsSummary());
                    }
                }
            }
        }
        throw new IllegalStateException("Survey did not reach /review within " + maxSections + " sections");
    }

    /** The section's invalid fields (tag, id, first label, error message), for failure messages. */
    private String invalidFieldsSummary() {
        Locator invalid = page.locator("[invalid]");
        return "invalid fields: " + invalid.count() + " " + invalid.evaluateAll(
                "els => els.slice(0, 12).map(e => e.tagName.toLowerCase() + '[' + e.id + ']:'"
                        + " + (e.querySelector('label')?.textContent || '') + ':'"
                        + " + (e.querySelector('[slot=error-message]')?.textContent || ''))");
    }

    /**
     * Answering one field can trigger a save-and-rebuild round trip (SectionView.saveAnswer)
     * that adds or removes downstream fields based on branching -- so the id/tag pairs are
     * snapshotted into a plain list up front, and each is re-checked for presence immediately
     * before acting: a field visible when this pass started can have already been removed by an
     * earlier field's answer within the *same* pass (confirmed live -- a radio group disappeared
     * mid-pass and a blind fill attempt hung waiting for it forever).
     */
    private void answerAllVisibleQuestions() {
        Locator fields = page.locator(".elicit-input-field");
        int count = fields.count();
        List<String> ids = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Locator field = fields.nth(i);
            String id = field.getAttribute("id");
            if (id == null || id.isEmpty()) {
                continue;
            }
            ids.add(id);
            tags.add((String) field.evaluate("el => el.tagName.toLowerCase()"));
        }
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            if (byId(id).count() == 0) {
                continue; // removed by branching from an earlier answer in this same pass
            }
            fillField(id, tags.get(i));
        }
    }

    private void fillField(String id, String tag) {
        switch (tag) {
            case "vaadin-text-field", "vaadin-email-field", "vaadin-password-field" -> input(id).fill("Test");
            case "vaadin-integer-field" -> input(id).fill("1");
            case "vaadin-date-picker" -> {
                input(id).fill("01/01/1995");
                input(id).press("Enter");
            }
            case "vaadin-combo-box" -> selectFirstComboOption(id);
            case "vaadin-radio-group" -> byId(id).locator("input[type=radio]").first().check();
            case "vaadin-checkbox" -> byId(id).locator("input[type=checkbox]").first().check();
            case "vaadin-multi-select-combo-box" -> selectFirstMultiSelectOption(id);
            default -> {
                // Rarer type (checkbox group, date/time picker, MODAL placeholder) --
                // deliberately left unanswered, see class Javadoc.
            }
        }
    }

    private void selectFirstComboOption(String id) {
        byId(id).locator("input").click();
        Locator item = page.locator("vaadin-combo-box-item").first();
        item.waitFor();
        item.click();
    }

    /**
     * Selects one option in a multi-select combo box, but only if it has none already: clicking
     * an already-selected overlay item toggles it back off, so re-visiting this field on a later
     * pass (once it's already answered) must be a no-op rather than re-clicking the same item --
     * confirmed empirically (re-clicking flipped the selection on/off across passes).
     *
     * <p>The "already answered" check reads the component's real {@code selectedItems} JS
     * property rather than counting rendered {@code <vaadin-multi-select-combo-box-chip>}
     * elements: this component can render a chip with no backing item ({@code item: null}, empty
     * text) before any selection is made, so a DOM chip count is not a reliable signal -- this
     * was confirmed against the live app (a field showing one such empty chip from first render,
     * with {@code selectedItems: []}) and cost real debugging time before being caught, so don't
     * revert to a chip-count check.</p>
     *
     * <p>The overlay's items are rendered in a virtualized list where a neighboring item's hit
     * area can overlap the target during Playwright's actionability check, so this force-clicks
     * rather than waiting for a clean, unobstructed hit target.</p>
     */
    private void selectFirstMultiSelectOption(String id) {
        boolean alreadyAnswered = (Boolean) byId(id).evaluate("el => el.selectedItems && el.selectedItems.length > 0");
        if (alreadyAnswered) {
            return;
        }
        Locator msInput = input(id);
        msInput.click();
        Locator item = page.locator("vaadin-multi-select-combo-box-item").first();
        item.waitFor();
        item.click(new Locator.ClickOptions().setForce(true));
        msInput.press("Escape");
    }

    /**
     * Clicks Next (an in-place rebuild, no URL change -- SectionView.nextSection() explicitly
     * avoids navigation) or Review (which navigates to /review when the section validates).
     *
     * <p>Both are the same {@code section-next-button}, captioned "Next" while a further
     * section exists and "Review" on the last one. The section (button included) is rebuilt
     * after the last answer's save round trip, so this <em>waits</em> for the button and then
     * reads its caption; a one-shot count of "Next" buttons taken mid-rebuild sees none and
     * wrongly concludes the survey is on its last section -- confirmed live against FHHS, where
     * it then spent all its Review retries on a page that plainly showed Next.</p>
     *
     * <p>Next has no URL change to wait on, but the rebuild is still an async server round trip:
     * without a settle wait here, the very next {@code answerAllVisibleQuestions()} pass can
     * read {@code .count()} against the outgoing section while the new section's elements are
     * only partially attached, then hang forever on a since-removed index -- confirmed live.
     * (A Next click that validation blocks stays on the same section, which the next pass
     * simply answers again -- indistinguishable from, and handled like, a real advance.)</p>
     */
    private Navigation proceedToNextSectionOrReview() {
        Locator navButton = byId("section-next-button");
        navButton.waitFor();
        page.waitForTimeout(300); // let a rebuild that has just started swap the button in
        navButton = byId("section-next-button");
        navButton.waitFor();
        if (nextCaption.equals(navButton.innerText().trim())) {
            clickAfterFill(navButton);
            page.waitForTimeout(500);
            return Navigation.NEXT_SECTION;
        }
        clickAfterFill(navButton);
        try {
            page.waitForURL(url -> url.contains("/review"), new Page.WaitForURLOptions().setTimeout(8000));
            return Navigation.REVIEW_REACHED;
        } catch (TimeoutError e) {
            // Validation kept us on the section (or a busy final section was slow to respond --
            // confirmed live after a large batch of repeated family-member sections). Either
            // way: answer again and retry.
            return Navigation.REVIEW_BLOCKED;
        }
    }

    /** The page this object drives, for chaining into the review page object. */
    public Page page() {
        return page;
    }

    /**
     * The document title. SectionView sets it to the section's display text on navigation only
     * (Next rebuilds in place), so it names the section the respondent <em>entered</em> on, not
     * necessarily the one shown; the review page lists every section's title instead.
     */
    public String title() {
        return page.title();
    }

    /**
     * How a field's own label is read. {@code :scope > label[slot=label]} first, because a group
     * component -- a radio group, a checkbox group -- contains its children's labels too, and a
     * plain {@code querySelector('label')} returns the first option's label instead of the
     * question's own (confirmed live: the race question read "White").
     */
    private static final String OWN_LABEL =
            "e => (e.querySelector(':scope > label[slot=\"label\"]') || e.querySelector('label'))";

    /** The visible labels of every question component on the section, in DOM order. */
    public List<String> labels() {
        Object result = page.locator(".elicit-input-field").evaluateAll(
                "els => els.map(e => ((" + OWN_LABEL + ")(e)?.textContent || e.textContent || '').trim())");
        return ((List<?>) result).stream().map(Object::toString).toList();
    }

    /** The label of the question component whose id is {@code displayKey} ("" if absent). */
    public String labelOf(String displayKey) {
        Locator field = byId(displayKey);
        if (field.count() == 0) {
            return "";
        }
        return (String) field.evaluate("e => ((" + OWN_LABEL + ")(e)?.textContent || '').trim()");
    }

    /**
     * Fills the question with id {@code displayKey} (a Survey display key such as
     * {@code 0001-0001-0000-0001-0000-0001-0000}) by its component type and gives the
     * save-and-rebuild round trip a moment: answering can add or remove fields (Survey UC-002
     * A3 repeats, SHOW rules), and the next locator must see the rebuilt section.
     */
    public void fillById(String displayKey, String value) {
        Locator field = byId(displayKey);
        field.waitFor();
        String tag = (String) field.evaluate("el => el.tagName.toLowerCase()");
        switch (tag) {
            case "vaadin-text-field", "vaadin-email-field", "vaadin-password-field", "vaadin-text-area",
                 "vaadin-integer-field", "vaadin-number-field" -> {
                input(displayKey).fill(value);
                input(displayKey).press("Tab");
            }
            case "vaadin-combo-box" -> {
                input(displayKey).click();
                Locator item = page.locator("vaadin-combo-box-item")
                        .filter(new Locator.FilterOptions().setHasText(value)).first();
                item.waitFor();
                item.click();
            }
            case "vaadin-radio-group" -> field.locator("vaadin-radio-button")
                    .filter(new Locator.FilterOptions().setHasText(value)).first().locator("input").check();
            case "vaadin-checkbox" -> field.locator("input[type=checkbox]").setChecked(Boolean.parseBoolean(value));
            // The three temporal pickers are set through the web component's own `value` property,
            // which is always ISO (yyyy-MM-dd, HH:mm, yyyy-MM-ddTHH:mm) whatever the page's
            // language, rather than by typing into the field: what a respondent may type there is
            // the locale's own format -- dd/MM/yyyy in Spanish, and Arabic-Indic digits in Arabic --
            // so a typed date would be a different string on every site. Setting the property fires
            // value-changed, which is what Flow listens to.
            case "vaadin-date-picker", "vaadin-time-picker", "vaadin-date-time-picker" ->
                    field.evaluate("(el, v) => { el.value = v; el.dispatchEvent(new CustomEvent('change', { bubbles: true })); }", value);
            default -> throw new IllegalStateException("fillById does not handle " + tag + " for " + displayKey);
        }
        page.waitForTimeout(600);
    }

    /**
     * Chooses answer options <em>by position</em> on the question with id {@code displayKey}, for
     * the choice types: radio group, combo box, checkbox group and multi-select combo box. Position
     * rather than label, so a test can answer the same survey in any language -- the labels are
     * survey content and are translated (Survey UC-009), the display order is not.
     *
     * <p>{@code indexes} are zero-based positions in the question's select group. A single-valued
     * type takes the first one and ignores the rest.</p>
     */
    public void chooseOptionsAt(String displayKey, int... indexes) {
        Locator field = byId(displayKey);
        field.waitFor();
        String tag = (String) field.evaluate("el => el.tagName.toLowerCase()");
        switch (tag) {
            case "vaadin-radio-group" -> field.locator("vaadin-radio-button").nth(indexes[0]).locator("input").check();
            case "vaadin-checkbox-group" -> {
                for (int index : indexes) {
                    field.locator("vaadin-checkbox").nth(index).locator("input[type=checkbox]").check();
                    page.waitForTimeout(300); // each toggle is its own save round trip
                }
            }
            case "vaadin-combo-box" -> {
                input(displayKey).click();
                Locator item = page.locator("vaadin-combo-box-item").nth(indexes[0]);
                item.waitFor();
                item.click();
            }
            case "vaadin-multi-select-combo-box" -> {
                Locator msInput = input(displayKey);
                msInput.click();
                for (int index : indexes) {
                    Locator item = page.locator("vaadin-multi-select-combo-box-item").nth(index);
                    item.waitFor();
                    // The overlay's list is virtualized and neighbors can overlap the hit area.
                    item.click(new Locator.ClickOptions().setForce(true));
                    page.waitForTimeout(200);
                }
                msInput.press("Escape");
            }
            default -> throw new IllegalStateException("chooseOptionsAt does not handle " + tag + " for " + displayKey);
        }
        page.waitForTimeout(600);
    }

    /**
     * The option labels of the choice question with id {@code displayKey}, in display order -- what
     * the respondent actually reads, so a multilingual test can assert the options were translated.
     * For the two combo-box types the overlay is opened to read its items and closed again.
     */
    public List<String> optionLabels(String displayKey) {
        Locator field = byId(displayKey);
        field.waitFor();
        String tag = (String) field.evaluate("el => el.tagName.toLowerCase()");
        switch (tag) {
            case "vaadin-radio-group" -> {
                return field.locator("vaadin-radio-button").allInnerTexts().stream().map(String::trim).toList();
            }
            case "vaadin-checkbox-group" -> {
                return field.locator("vaadin-checkbox").allInnerTexts().stream().map(String::trim).toList();
            }
            case "vaadin-combo-box", "vaadin-multi-select-combo-box" -> {
                String item = "vaadin-combo-box".equals(tag) ? "vaadin-combo-box-item" : "vaadin-multi-select-combo-box-item";
                Locator comboInput = input(displayKey);
                comboInput.click();
                Locator items = page.locator(item);
                items.first().waitFor();
                // The overlay's list is virtualized: an item element is attached first and its
                // label written into it by the renderer a tick later, so a read taken the moment
                // the first item appears comes back as a list of empty strings (confirmed live).
                page.waitForFunction("selector => { const els = document.querySelectorAll(selector);"
                        + " return els.length > 0 && [...els].every(e => e.textContent.trim().length > 0); }", item);
                List<String> labels = items.allInnerTexts().stream().map(String::trim).toList();
                comboInput.press("Escape");
                page.waitForTimeout(200);
                return labels;
            }
            default -> throw new IllegalStateException("optionLabels does not handle " + tag + " for " + displayKey);
        }
    }

    /**
     * Closes every MODAL question currently open on the section. A {@code MODAL} question is an
     * {@code ElicitModal} dialog that opens itself the moment the section attaches, with a close
     * button whose id is the question's display key plus {@code -close}; until it is closed it
     * covers the section's own buttons.
     */
    public void closeModalQuestions() {
        // A MODAL question's close button is the only element in Survey whose id ends in "-close"
        // (ElicitModal.CLOSE_BUTTON_ID_SUFFIX), so this is specific without having to guess where
        // Vaadin renders it: a Dialog's contents stay in the light DOM of the vaadin-dialog host
        // and are only portalled into the overlay visually, so scoping to vaadin-dialog-overlay
        // finds nothing at all.
        Locator closes = page.locator("vaadin-button[id$=\"-close\"]:visible");
        for (int i = 0; i < 5 && closes.count() > 0; i++) {
            closes.first().click();
            page.waitForTimeout(400);
        }
        if (closes.count() > 0) {
            throw new IllegalStateException("A MODAL question would not close on " + page.url());
        }
    }

    /**
     * The full display key of the question whose id ends with {@code keySuffix} -- the key
     * without its leading survey-id group, so callers need not know which survey id the site
     * assigned. Waits for the field to appear (answers can create it via a rule).
     */
    public String keyEndingWith(String keySuffix) {
        Locator field = page.locator(".elicit-input-field[id$=\"" + keySuffix + "\"]").first();
        field.waitFor();
        return field.getAttribute("id");
    }

    /** True if a question whose id ends with {@code keySuffix} is currently on the section. */
    public boolean hasKeyEndingWith(String keySuffix) {
        return page.locator(".elicit-input-field[id$=\"" + keySuffix + "\"]").count() > 0;
    }

    /** The current value of the question with id {@code displayKey} (text-like fields and combo boxes). */
    public String valueOf(String displayKey) {
        return input(displayKey).inputValue();
    }

    /**
     * Clicks Next (an in-place rebuild, no URL change -- the document title does not change
     * either, Vaadin only refreshes it on navigation) and waits until the section's fields have
     * been replaced. A save round trip that is still finishing (the last answer typed can create
     * whole new steps) rebuilds the section including its buttons, so a click that lands on the
     * outgoing button is lost (confirmed live); if the fields have not changed after a few
     * seconds the click is repeated once.
     */
    public void next() {
        List<String> before = fieldIds();
        clickNextButton();
        if (!waitForFieldsChange(before, 4_000)) {
            clickNextButton();
            if (!waitForFieldsChange(before, 10_000)) {
                throw new IllegalStateException("Next did not leave the section with fields " + before + " on " + page.url());
            }
        }
    }

    /** The ids (display keys) of the question components currently on the section. */
    public List<String> fieldIds() {
        Object ids = page.locator(".elicit-input-field").evaluateAll("els => els.map(e => e.id)");
        return ((List<?>) ids).stream().map(Object::toString).toList();
    }

    private void clickNextButton() {
        closeModalQuestions(); // see review(): a rebuilt section re-opens its MODAL over the buttons
        Locator navButton = byId("section-next-button");
        navButton.waitFor();
        page.waitForTimeout(300);
        navButton = byId("section-next-button");
        navButton.waitFor();
        if (!nextCaption.equals(navButton.innerText().trim())) {
            throw new IllegalStateException("Expected the '" + nextCaption + "' button but the section shows '"
                    + navButton.innerText().trim() + "'");
        }
        navButton.click();
    }

    private boolean waitForFieldsChange(List<String> before, int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            List<String> now = fieldIds();
            if (!now.isEmpty() && !now.equals(before)) {
                page.waitForTimeout(300); // let the new section finish attaching
                return true;
            }
            page.waitForTimeout(200);
        }
        return false;
    }

    /** Clicks Review on the last section and waits for the review page. */
    public void review() {
        // A MODAL question re-opens itself every time the section is rebuilt (ElicitModal opens on
        // attach), so one closed a moment ago can be covering this button again.
        closeModalQuestions();
        Locator navButton = byId("section-next-button");
        navButton.waitFor();
        page.waitForTimeout(300);
        String caption = navButton.innerText().trim();
        if (!reviewCaption.equals(caption)) {
            throw new IllegalStateException("Expected the '" + reviewCaption + "' button on the last section but it shows '"
                    + caption + "' (fields " + fieldIds() + ")");
        }
        clickAfterFill(navButton);
        page.waitForURL(url -> url.contains("/review"));
    }

    /** True when the navigation button reads "Review", i.e. this is the survey's last section. */
    public boolean onLastSection() {
        Locator navButton = byId("section-next-button");
        navButton.waitFor();
        return reviewCaption.equals(navButton.innerText().trim());
    }
}
