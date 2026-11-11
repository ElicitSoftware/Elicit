package com.elicitsoftware.e2e.survey;

import com.elicitsoftware.e2e.PageObject;
import com.microsoft.playwright.Page;

/** ReviewView (Survey UC-003; Finish triggers UC-004 finalize and navigates to /report). */
public class ReviewPage extends PageObject {

    public ReviewPage(Page page) {
        super(page);
    }

    /** The section titles the review lists (one {@code h4} per section card, e.g. "About Alice:"). */
    public java.util.List<String> sectionTitles() {
        page.locator("h4").first().waitFor();
        return page.locator("h4").allInnerTexts().stream().map(String::trim).toList();
    }

    public void finish() {
        byId("review-finish-button").click();
        page.waitForURL(url -> url.contains("/report"));
    }
}
