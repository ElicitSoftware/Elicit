package com.elicitsoftware.e2e.survey;

import com.elicitsoftware.e2e.PageObject;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * ReportView (Survey UC-005). Generate PDF opens the download in a new browser tab. A survey that
 * lists no reports has no report page (BR-014): no cards, no Generate PDF, and a completion notice
 * when the survey has no post-survey URL to redirect to.
 */
public class ReportPage extends PageObject {

    public ReportPage(Page page) {
        super(page);
    }

    public void generatePdf() {
        generatePdfButton().click();
    }

    public Locator generatePdfButton() {
        return byId("report-generate-pdf-button");
    }

    /** The notice shown in place of reports when the survey lists none and has no post-survey URL. */
    public Locator completionNotice() {
        return byId("report-complete-notice");
    }
}
