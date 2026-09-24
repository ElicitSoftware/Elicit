package com.elicitsoftware.e2e.admin;

import com.elicitsoftware.e2e.PageObject;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.nio.file.Path;

/**
 * RespondentImportView (Admin UC-012, route {@code /respondent-import}): uploads a respondent
 * export produced by the Search view's "Export" action (Admin UC-011). Like
 * {@link SurveyApplyPage}, the outcome is a modal dialog titled "Import Successful" or "Import
 * Failed" whose full text is returned for the caller to assert on, then closed.
 */
public class RespondentImportPage extends PageObject {

    public RespondentImportPage(Page page) {
        super(page);
    }

    /**
     * Uploads {@code exportFile} and returns the result dialog's text. The upload component is
     * given a moment to initialise after navigation before the file is set (setting it during
     * the component's own start-up left the file listed but never produced the dialog, seen
     * live), and the dialog is allowed a full minute since the import runs inside the upload
     * request; on timeout the upload widget's state is reported.
     */
    public String importFile(Path exportFile) {
        Locator upload = page.locator("#respondent-import-upload");
        upload.waitFor();
        page.waitForTimeout(500);
        upload.locator("input[type=file]").setInputFiles(exportFile);
        Locator dialog = page.locator("vaadin-dialog[opened]");
        try {
            dialog.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED).setTimeout(60_000));
        } catch (com.microsoft.playwright.TimeoutError e) {
            throw new IllegalStateException("No result dialog after uploading " + exportFile.getFileName()
                    + "; upload widget shows: " + upload.innerText().replace('\n', '|'), e);
        }
        String text = dialog.innerText();
        dialog.locator("vaadin-button").filter(new Locator.FilterOptions().setHasText("Close")).click();
        dialog.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED));
        return text;
    }
}
