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

    public SectionPage(Page page) {
        super(page);
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
        if ("Next".equals(navButton.innerText().trim())) {
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

    /** The visible labels of every question component on the section, in DOM order. */
    public List<String> labels() {
        Object result = page.locator(".elicit-input-field").evaluateAll(
                "els => els.map(e => (e.querySelector('label')?.textContent || e.textContent || '').trim())");
        return ((List<?>) result).stream().map(Object::toString).toList();
    }

    /** The label of the question component whose id is {@code displayKey} ("" if absent). */
    public String labelOf(String displayKey) {
        Locator field = byId(displayKey);
        if (field.count() == 0) {
            return "";
        }
        return (String) field.evaluate("e => (e.querySelector('label')?.textContent || '').trim()");
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
            default -> throw new IllegalStateException("fillById does not handle " + tag + " for " + displayKey);
        }
        page.waitForTimeout(600);
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
        Locator navButton = byId("section-next-button");
        navButton.waitFor();
        page.waitForTimeout(300);
        navButton = byId("section-next-button");
        navButton.waitFor();
        if (!"Next".equals(navButton.innerText().trim())) {
            throw new IllegalStateException("Expected a Next button but the section shows '" + navButton.innerText().trim() + "'");
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
        Locator navButton = byId("section-next-button");
        navButton.waitFor();
        clickAfterFill(navButton);
        page.waitForURL(url -> url.contains("/review"));
    }
}
