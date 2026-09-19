package com.elicitsoftware.e2e.author;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.nio.file.Path;

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
        Locator dialog = topDialog();
        fieldByLabel(dialog, "Initial display key").fill(displayKey);
        submitDialog(dialog, "Save");
        page.locator("vaadin-notification-card")
                .filter(new Locator.FilterOptions().setHasText("Survey updated"))
                .waitFor();
    }

    /** True once the validation panel reports nothing blocking or noteworthy. */
    public boolean isReadyToExport() {
        return page.getByText("Ready to export: no findings.").count() > 0;
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
        Locator dialog = topDialog();
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
