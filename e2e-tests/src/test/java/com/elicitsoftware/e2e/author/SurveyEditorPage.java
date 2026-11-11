package com.elicitsoftware.e2e.author;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * SurveyEditorView (Author UC-007 validation, UC-008 export) -- the survey overview at
 * {@code /survey/{id}} with its validation panel and the Edit details / Export dialogs.
 *
 * <p>The exported {@code .elicit} file is delivered by a Vaadin download {@code Anchor} that the
 * Export dialog renders after a successful export (not a popup window, unlike the apps' PDF
 * flows), so {@link #export} captures it with Playwright's download event.</p>
 */
public class SurveyEditorPage extends AuthorPageObject {

    public SurveyEditorPage(Page page) {
        super(page);
    }

    /**
     * The display key of the first section assignment of the first step, as
     * {@code DisplayOrdering} in Author derives it. It is never shown in the UI, and export
     * refuses a survey whose initial display key does not name a current section assignment.
     */
    public static String firstSectionDisplayKey(int surveyId) {
        return String.format("%04d-0001-0000-0001-0000-0000-0000", surveyId);
    }

    /** Edit details: sets the "Initial display key" the Survey runtime enters the survey at. */
    public void setInitialDisplayKey(String displayKey) {
        buttonByText("Edit details").click();
        Locator dialog = dialog("Edit survey");
        fieldByLabel(dialog, "Initial display key").fill(displayKey);
        submitDialog(dialog, "Save");
        page.locator("vaadin-notification-card")
                .filter(new Locator.FilterOptions().setHasText("Survey updated")).first().waitFor();
    }

    /**
     * Edit details (Author UC-044): sets the survey's published translations to exactly the
     * comma-separated BCP-47 tags given, and optionally the base language the content is written
     * in ("Content language"; null leaves it alone).
     *
     * <p>"Published translations" is a {@code MultiSelectComboBox} offering
     * {@code author.content.languages} -- the languages Elicit ships application text for -- so a
     * tag is <em>selected</em>, not typed. That is the point of the control: a survey may only
     * publish content in a language some site could render (UC-044 BR-004), and it used to be a
     * free-text field that let an author publish one no respondent would ever see.</p>
     *
     * <p>Items toggle on click, so this walks the whole list and clicks only the ones whose
     * selected state is wrong -- which sets and unsets in one pass, and leaves a tag the survey
     * already publishes alone.</p>
     */
    public void setContentLanguages(String publishedTags, String baseLanguage) {
        buttonByText("Edit details").click();
        Locator dialog = dialog("Edit survey");
        if (baseLanguage != null) {
            fieldByLabel(dialog, "Content language").fill(baseLanguage);
        }

        List<String> wanted = Arrays.stream(publishedTags.split(","))
                .map(String::trim).filter(tag -> !tag.isEmpty()).toList();
        Locator picker = languagePicker(dialog);
        openPicker(picker);
        Locator items = pickerItems(picker);
        List<String> labels = items.allInnerTexts().stream().map(String::trim).toList();
        List<String> offered = labels.stream().map(SurveyEditorPage::tagOf).toList();
        for (String tag : wanted) {
            if (!offered.contains(tag)) {
                throw new IllegalStateException("Published translations offers no '" + tag
                        + "'; Elicit carries " + offered + ". A survey may only publish a language"
                        + " the applications have text for (UC-044 BR-004).");
            }
        }
        for (int i = 0; i < labels.size(); i++) {
            boolean selected = items.nth(i).getAttribute("selected") != null;
            if (selected != wanted.contains(tagOf(labels.get(i)))) {
                items.nth(i).click();
                page.waitForTimeout(150);
            }
        }
        closePicker(picker);

        submitDialog(dialog, "Save");
        page.locator("vaadin-notification-card")
                .filter(new Locator.FilterOptions().setHasText("Survey updated")).first().waitFor();
    }

    /**
     * The published translations the Edit details dialog currently holds (UC-044 step 3), as tags
     * in the order the picker shows them.
     *
     * <p>Read from the chips rather than the items, so it reports what the field holds without
     * opening the list. The overflow chip the component always keeps in the DOM carries no label,
     * so it drops out with the blank filter.</p>
     */
    public List<String> contentLanguages() {
        buttonByText("Edit details").click();
        Locator dialog = dialog("Edit survey");
        List<String> tags = languagePicker(dialog)
                .locator("vaadin-multi-select-combo-box-chip:not([slot=overflow])")
                .allInnerTexts().stream()
                .map(String::trim).filter(text -> !text.isEmpty())
                .map(SurveyEditorPage::tagOf).toList();
        buttonByText(dialog, "Cancel").click();
        return tags;
    }

    // ---- the Published translations picker ----------------------------------------------------

    private Locator languagePicker(Locator dialog) {
        Locator picker = dialog.locator(
                "vaadin-multi-select-combo-box:has(label:text-is(\"Published translations\"))").first();
        picker.waitFor();
        return picker;
    }

    /**
     * Opens the picker's list. Its toggle button carries the two-valued part
     * {@code "field-button toggle-button"}, so it takes {@code [part~=...]}; the button is in the
     * component's shadow root, which Playwright pierces for a plain CSS selector.
     */
    private void openPicker(Locator picker) {
        if (pickerItems(picker).count() == 0) {
            picker.locator("[part~=\"toggle-button\"]").click();
        }
        pickerItems(picker).first().waitFor();
    }

    private void closePicker(Locator picker) {
        page.keyboard().press("Escape");
        // Hidden, not detached: closing the list leaves every item in the DOM marked hidden.
        picker.locator("vaadin-multi-select-combo-box-item").first()
                .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
    }

    /**
     * The items the list is showing. Like the single-select combo box, this overlay is not
     * portaled to {@code body}: it sits in the component's shadow root and slots a scroller from
     * the component's own light DOM, and closing or filtering only hides the items.
     */
    private Locator pickerItems(Locator picker) {
        return picker.locator("vaadin-multi-select-combo-box-item:visible");
    }

    /** The tag out of an item or chip label, which reads "{@code es-419 — Spanish (Latin America)}". */
    private static String tagOf(String label) {
        int dash = label.indexOf('\u2014');
        return (dash < 0 ? label : label.substring(0, dash)).trim();
    }

    /** Navigates to the survey's Translations page through the toolbar (caption "Translations…"). */
    public void openTranslations() {
        buttonByText("Translations").click(); // the caption is "Translations…" (U+2026)
        page.waitForURL(url -> url.endsWith("/translations"));
    }

    /** Navigates to the survey's designer board through the toolbar (caption "Design…"). */
    public void openDesigner() {
        buttonByText("Design").click(); // the caption is "Design…" (U+2026)
        page.waitForURL(url -> url.endsWith("/design"));
    }

    /**
     * Waits until the validation panel has finished rendering -- either the "ready" line or at
     * least one finding. The panel is built during the same server round trip as the rest of the
     * view, so this only guards against asserting on a half-drawn page.
     */
    public void waitForValidation() {
        page.locator("h3:has-text(\"Validation\")").first().waitFor();
        page.locator("h3:has-text(\"Validation\") ~ p, h3:has-text(\"Validation\") ~ ul li").first().waitFor();
    }

    /** True once the validation panel reports nothing blocking or noteworthy. */
    public boolean isReadyToExport() {
        return page.getByText("Ready to export. No problems found.").count() > 0;
    }

    /**
     * Waits for the validation panel's clean verdict. {@link #isReadyToExport()} does not wait, so
     * after opening the editor by URL the panel may not have rendered yet; this does.
     */
    public void waitUntilReadyToExport() {
        page.getByText("Ready to export. No problems found.").first().waitFor();
    }

    /** The validation panel's text, for a readable assertion message. */
    public String validationText() {
        return page.locator("h3:has-text(\"Validation\")").locator("..").innerText();
    }

    /**
     * Export…: enters the mandatory release note, exports, and saves the resulting download
     * into {@code targetDir}. Returns the saved file (named as Author named it).
     */
    public Path export(String releaseNote, Path targetDir) {
        buttonByText("Export").click(); // the caption is "Export…" (U+2026)
        Locator dialog = dialogTitledLike("Export ");
        fieldByLabel(dialog, "Release note").fill(releaseNote);
        Locator exportButton = buttonByText(dialog, "Export");
        clickAfterFill(exportButton);

        Locator link = dialog.locator("a[download]");
        link.waitFor(new Locator.WaitForOptions().setTimeout(15_000));
        Download download = page.waitForDownload(link::click);
        Path saved = targetDir.resolve(download.suggestedFilename());
        download.saveAs(saved);
        buttonByText(dialog, "Close").click();
        return saved;
    }
}
