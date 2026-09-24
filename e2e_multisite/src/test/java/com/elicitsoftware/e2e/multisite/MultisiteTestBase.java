package com.elicitsoftware.e2e.multisite;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

/**
 * Playwright lifecycle for the two-site journey. Mirrors {@code com.elicitsoftware.e2e.E2ETestBase}
 * (the single-stack suite this module borrows its page objects from) but knows two sites, and
 * hands out one fresh {@link BrowserContext} per persona instead of one page per test: the
 * journey is a single ordered story in which the author, each site's administrator and every
 * respondent visit must never share cookies. Both sites share one Keycloak and one
 * {@code admin} login, so a leaked SSO session would be harmless, but separate contexts keep
 * the phases independent of each other.
 *
 * <p>Targets already-running stacks (see README.md and ./up.sh); nothing is started here.</p>
 */
public abstract class MultisiteTestBase {

    protected static final Site SITE1 = Site.fromProperties("site1", "site 1",
            "http://localhost:8080", "http://localhost:8081", "http://localhost:8084");
    protected static final Site SITE2 = Site.fromProperties("site2", "site 2",
            "http://localhost:8030", "http://localhost:8031", null);

    protected static final String ADMIN_USERNAME = System.getProperty("admin.username", "admin");
    protected static final String ADMIN_PASSWORD = System.getProperty("admin.password", "admin");
    protected static final String AUTHOR_USERNAME = System.getProperty("author.username", "author");
    protected static final String AUTHOR_PASSWORD = System.getProperty("author.password", "author");

    private static Playwright playwright;
    private static Browser browser;

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

    /**
     * Whether the no-department dialog (Admin UC-028) is on screen, detected by a button in its
     * footer: Vaadin renders a Dialog's contents into an overlay element, so the id set on the
     * Dialog itself is not what the page shows.
     */
    protected static boolean isBlockingDepartmentDialogOpen(Page page) {
        Locator logout = page.locator("#missing-department-logout");
        return logout.count() > 0 && logout.first().isVisible();
    }

    /** A fresh, cookie-less context; callers close it when the persona's visit is over. */
    protected static BrowserContext newContext() {
        return browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 1024));
    }

    /** Runs {@code visit} with a page in a fresh context, closing the context afterwards. */
    protected static void visit(Visit visit) {
        BrowserContext context = newContext();
        Page page = context.newPage();
        try {
            visit.run(page);
        } catch (Exception | Error e) {
            // Keep a picture of where the story broke: target/failure-<time>.png, named in the message.
            java.nio.file.Path shot = java.nio.file.Path.of("target", "failure-" + System.currentTimeMillis() + ".png");
            try {
                page.screenshot(new Page.ScreenshotOptions().setPath(shot).setFullPage(true));
            } catch (RuntimeException ignored) {
                shot = null;
            }
            String where = "at " + page.url() + (shot == null ? "" : " (screenshot " + shot + ")");
            if (e instanceof AssertionError ae) {
                throw new AssertionError(ae.getMessage() + " " + where, ae);
            }
            throw new IllegalStateException(e.getMessage() + " " + where, e);
        } finally {
            context.close();
        }
    }

    @FunctionalInterface
    protected interface Visit {
        void run(Page page) throws Exception;
    }

    protected static void openAdmin(Page page, Site site, String path) {
        page.navigate(site.adminBaseUrl() + path);
    }

    protected static void openSurvey(Page page, Site site, String path) {
        page.navigate(site.surveyBaseUrl() + path);
    }

    protected static void openAuthor(Page page, Site site, String path) {
        if (!site.hasAuthor()) {
            throw new IllegalArgumentException(site + " has no Author");
        }
        page.navigate(site.authorBaseUrl() + path);
    }
}
