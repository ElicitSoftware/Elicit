package com.elicitsoftware.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

/**
 * Shared Playwright lifecycle and config for the cross-app browser test suite.
 *
 * <p>Targets an already-running docker-compose stack (Admin, Survey, Keycloak) -- this class
 * never starts or manages either app itself. Base URLs and Admin credentials default to the
 * values in {@code docker-compose.yml} / the Keycloak {@code elicit} realm, overridable via
 * {@code -D} system properties for CI (see {@code pom.xml}).</p>
 */
public abstract class E2ETestBase {

    protected static final String ADMIN_BASE_URL = System.getProperty("admin.baseUrl", "http://localhost:8081");
    protected static final String SURVEY_BASE_URL = System.getProperty("survey.baseUrl", "http://localhost:8080");
    protected static final String ADMIN_USERNAME = System.getProperty("admin.username", "admin");
    protected static final String ADMIN_PASSWORD = System.getProperty("admin.password", "admin");

    private static Playwright playwright;
    private static Browser browser;

    protected BrowserContext context;
    protected Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        boolean headless = !"false".equals(System.getProperty("e2e.headless"));
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setArgs(List.of("--window-size=1440,1024")));
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void createPage() {
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 1024));
        page = context.newPage();
    }

    @AfterEach
    void closePage() {
        if (context != null) {
            context.close();
        }
    }

    protected void openAdmin(String path) {
        page.navigate(ADMIN_BASE_URL + path);
    }

    protected void openSurvey(String path) {
        page.navigate(SURVEY_BASE_URL + path);
    }

    /**
     * Runs {@code action} and returns the new tab/popup it opens. Both apps' PDF-generation
     * flows use {@code window.open(url, '_blank')}, which Playwright surfaces as a "popup" event
     * on the originating page.
     */
    protected Page waitForPopup(Runnable action) {
        return page.waitForPopup(action::run);
    }
}
