package com.elicitsoftware.e2e.author;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ImportDefinitionView (Author UC-005) -- the {@code /import} route that loads an
 * {@code ELICIT_SURVEY_EXPORT_V1} file into Author as a working copy.
 *
 * <p>Unlike Admin's apply and respondent-import views, the outcome is rendered <em>into the page</em>
 * (a heading, the survey key and record lines, and an "Open in editor" button) rather than into a
 * modal dialog, so {@link #importFile} waits for that block and returns its text. A failure is a
 * paragraph reading "Import failed: ...", which is returned the same way for the caller to assert
 * on.</p>
 */
public class ImportDefinitionPage extends AuthorPageObject {

    private static final Pattern SURVEY_URL = Pattern.compile("/survey/(\\d+)$");

    public ImportDefinitionPage(Page page) {
        super(page);
    }

    /**
     * Uploads {@code elicitFile} and returns what the view reported: on success the "Imported
     * {name}" heading with the survey key, file revision and record counts; on failure the
     * "Import failed: ..." paragraph. The import runs inside the upload request, so this allows
     * it a full minute.
     */
    public String importFile(Path elicitFile) {
        Locator upload = page.locator("vaadin-upload").first();
        upload.waitFor();
        page.waitForTimeout(500); // setting the file during the component's own start-up is lost
        upload.locator("input[type=file]").setInputFiles(elicitFile);
        Locator outcome = page.locator("h3, p:has-text(\"Import failed\")")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("Imported |Import failed")));
        outcome.first().waitFor(new Locator.WaitForOptions().setTimeout(60_000));
        // The record lines follow the heading a moment later, in the same server response.
        page.waitForTimeout(500);
        return page.locator("vaadin-vertical-layout, body").first().innerText();
    }

    /** True once the import succeeded and offers to open the new working copy. */
    public boolean succeeded() {
        return buttonByText("Open in editor").count() > 0;
    }

    /** UC-005 step 7: clicks "Open in editor" and returns the id Author gave the working copy. */
    public int openInEditor() {
        buttonByText("Open in editor").click();
        page.waitForURL(SURVEY_URL);
        Matcher m = SURVEY_URL.matcher(page.url());
        if (!m.find()) {
            throw new IllegalStateException("Unexpected survey URL after Open in editor: " + page.url());
        }
        return Integer.parseInt(m.group(1));
    }
}
