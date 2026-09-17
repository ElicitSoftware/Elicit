package com.elicitsoftware.e2e.survey;

import com.elicitsoftware.e2e.PageObject;
import com.microsoft.playwright.Page;

/** MainView (Survey UC-001). Assumes a single configured survey (no survey selector shown). */
public class LoginPage extends PageObject {

    public LoginPage(Page page) {
        super(page);
    }

    public void loginWithToken(String token) {
        input("login-token-field").fill(token);
        clickAfterFill(byId("login-button"));
        // A valid token triggers a server round trip that navigates to /section or /report --
        // wait for it rather than letting the caller read page.url() before it happens.
        page.waitForURL(url -> !url.contains("/login"));
    }
}
