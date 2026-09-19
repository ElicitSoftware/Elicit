package com.elicitsoftware.e2e.survey;

import com.elicitsoftware.e2e.PageObject;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * MainView (Survey UC-001). With a single installed survey no survey selector is shown; once a
 * second survey exists MainView renders a "Surveys" combo box ({@code login-survey-select}) that
 * <em>must</em> be chosen before Login -- access codes are only unique within a survey, and the
 * session's survey id stays unset until the combo box is used.
 */
public class LoginPage extends PageObject {

    public LoginPage(Page page) {
        super(page);
    }

    /** Logs in when only one survey is installed (no survey selector rendered). */
    public void loginWithAccessCode(String accessCode) {
        input("login-access-code-field").fill(accessCode);
        submit();
    }

    /**
     * Logs in, choosing {@code surveyName} in the survey selector first when one is rendered
     * (i.e. when the stack has more than one survey installed) -- so callers can name the
     * survey they expect without caring how many are installed.
     */
    public void loginWithAccessCode(String surveyName, String accessCode) {
        // Vaadin renders the form after page load: wait for it before probing for the
        // selector, otherwise count() runs against a still-empty page and the selector is
        // silently skipped (leaving the session's survey unset, so Login then fails).
        input("login-access-code-field").waitFor();
        if (byId("login-survey-select").count() > 0) {
            selectSurvey(surveyName);
        }
        input("login-access-code-field").fill(accessCode);
        submit();
    }

    private void selectSurvey(String surveyName) {
        input("login-survey-select").click();
        Locator item = page.locator("vaadin-combo-box-item")
                .filter(new Locator.FilterOptions().setHasText(surveyName)).first();
        item.waitFor();
        item.click();
    }

    private void submit() {
        clickAfterFill(byId("login-button"));
        // A valid access code triggers a server round trip that navigates to /section or
        // /report -- wait for it rather than letting the caller read page.url() before it happens.
        page.waitForURL(url -> !url.contains("/login"));
    }
}
