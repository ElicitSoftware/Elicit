package com.elicitsoftware.e2e.multilingual;

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
 * Playwright lifecycle for the three-site multilingual journey. Mirrors
 * {@code com.elicitsoftware.e2e.multisite.MultisiteTestBase} but knows three sites and cares about
 * the language each one serves.
 *
 * <p>Every persona visit (the author, each site's administrator, every respondent login) runs in a
 * fresh {@link BrowserContext}. That matters more here than in the English suite: the chosen
 * language is remembered in the <em>browser session</em> (Survey UC-009 step 5), so a respondent
 * context that outlived its phase would carry Spanish into an English visit.</p>
 *
 * <p>Targets already-running stacks (see README.md and ./up.sh); nothing is started here.</p>
 */
public abstract class MultilingualTestBase {

    /**
     * The one viewport every context uses, and therefore the frame size of a recording
     * ({@link Recording}). 1440x1024 is wide enough for Author's designer board and tall enough
     * for a section page without scrolling, and both sides are even, which is what h264 wants.
     */
    protected static final int VIEWPORT_WIDTH = 1440;
    protected static final int VIEWPORT_HEIGHT = 1024;

    protected static final Site USA = Site.usa();
    protected static final Site MEXICO = Site.mexico();
    protected static final Site ARABIA = Site.arabia();
    protected static final List<Site> REMOTE_SITES = List.of(MEXICO, ARABIA);
    protected static final List<Site> ALL_SITES = List.of(USA, MEXICO, ARABIA);

    protected static final String ADMIN_USERNAME = System.getProperty("admin.username", "admin");
    protected static final String ADMIN_PASSWORD = System.getProperty("admin.password", "admin");
    protected static final String AUTHOR_USERNAME = System.getProperty("author.username", "author");
    protected static final String AUTHOR_PASSWORD = System.getProperty("author.password", "author");

    private static Playwright playwright;
    private static Browser browser;

    @BeforeAll
    static void launchBrowser() {
        Recording.start();
        playwright = Playwright.create();
        boolean headless = !"false".equals(System.getProperty("e2e.headless"));
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setArgs(List.of("--window-size=" + VIEWPORT_WIDTH + "," + VIEWPORT_HEIGHT));
        // A recording run is slowed down on purpose; see Recording#slowMo.
        Recording.decorate(options);
        browser = playwright.chromium().launch(options);
    }

    @AfterAll
    static void closeBrowser() {
        Recording.finish();
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

    /**
     * A fresh, cookie-less context. Accept-Language is pinned to English so that a respondent's
     * language is only ever the one the journey asked for, never one Vaadin negotiated from the
     * browser (UC-009 step 2): the {@code ?lang=} in the link has to be what decides.
     */
    protected static BrowserContext newContext() {
        Browser.NewContextOptions options = new Browser.NewContextOptions()
                .setViewportSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
                .setLocale("en-US");
        // One clip per context, which is one persona visit; inert unless -De2e.record=true.
        Recording.decorate(options, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        BrowserContext context = browser.newContext(options);
        Recording.attach(context);
        return context;
    }

    /** Runs {@code visit} with a page in a fresh context, closing the context afterwards. */
    protected static void visit(Visit visit) {
        BrowserContext context = newContext();
        Page page = context.newPage();
        Recording.opened(page);
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
            // Ends the clip and closes the context, in that order (Recording#closeContext).
            Recording.closeContext(context, page);
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

    /** The {@code dir} attribute of the document element -- "rtl" on an Arabic page (UC-009 step 3). */
    protected static String documentDirection(Page page) {
        return String.valueOf(page.evaluate("() => document.documentElement.getAttribute('dir') || 'ltr'"));
    }
}
