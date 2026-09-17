package com.elicitsoftware.e2e.survey;

import com.elicitsoftware.e2e.PageObject;
import com.microsoft.playwright.Page;

/** ReportView (Survey UC-005). Generate PDF opens the download in a new browser tab. */
public class ReportPage extends PageObject {

    public ReportPage(Page page) {
        super(page);
    }

    public void generatePdf() {
        byId("report-generate-pdf-button").click();
    }
}
