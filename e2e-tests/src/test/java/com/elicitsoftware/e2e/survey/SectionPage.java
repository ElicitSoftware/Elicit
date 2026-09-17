package com.elicitsoftware.e2e.survey;

import com.elicitsoftware.e2e.PageObject;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

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

    /** Repeatedly fills whatever is visible and advances until the Review page is reached. */
    public void answerSurveyUntilReview() {
        // FHHS-style surveys can legitimately spawn one set of sections per family member via
        // repeat-count branching (Survey UC-002 A3), so this needs real headroom, not just a
        // runaway-loop guard.
        int maxSections = 200;
        for (int i = 0; i < maxSections; i++) {
            answerAllVisibleQuestions();
            if (proceedToNextSectionOrReview()) {
                return; // clicked Review and its navigation to /review completed
            }
        }
        throw new IllegalStateException("Survey did not reach /review within " + maxSections + " sections");
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
     * avoids navigation) or Review (which does navigate to /review). Returns {@code true} once
     * Review was clicked and the resulting navigation has completed.
     *
     * <p>Next has no URL change to wait on, but the rebuild is still an async server round trip:
     * without a settle wait here, the very next {@code answerAllVisibleQuestions()} pass can
     * read {@code .count()} against the outgoing section while the new section's elements are
     * only partially attached, then hang forever on a since-removed index -- confirmed live.</p>
     */
    private boolean proceedToNextSectionOrReview() {
        Locator next = page.locator("vaadin-button").filter(new Locator.FilterOptions().setHasText("Next"));
        if (next.count() > 0) {
            clickAfterFill(next.first());
            page.waitForTimeout(500);
            return false;
        }
        clickReviewWithRetries();
        return true;
    }

    /**
     * Clicks Review and waits for the resulting {@code /review} navigation, retrying on a busy
     * final section (e.g. one following a large batch of repeated family-member sections):
     * confirmed live that the very first click attempt can time out there even though the button
     * itself is present and eventually clickable a moment later.
     */
    private void clickReviewWithRetries() {
        Locator review = page.locator("vaadin-button").filter(new Locator.FilterOptions().setHasText("Review")).first();
        page.waitForTimeout(1500);
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                review.click(new Locator.ClickOptions().setTimeout(8000));
                page.waitForURL(url -> url.contains("/review"), new Page.WaitForURLOptions().setTimeout(8000));
                return;
            } catch (Exception e) {
                if (attempt == 4) {
                    throw e;
                }
                page.waitForTimeout(1500);
            }
        }
    }
}
