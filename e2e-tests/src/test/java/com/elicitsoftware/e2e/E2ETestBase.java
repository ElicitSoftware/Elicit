package com.elicitsoftware.e2e;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import com.elicitsoftware.e2e.admin.DepartmentsPage;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Shared Playwright lifecycle and config for the cross-app browser test suite.
 *
 * <p>Targets an already-running docker-compose stack (Author, Admin, Survey, Keycloak) -- this
 * class never starts or manages any app itself. Base URLs and credentials default to the
 * values in {@code docker-compose.yml} / the Keycloak {@code elicit} realm, overridable via
 * {@code -D} system properties for CI (see {@code pom.xml}).</p>
 */
public abstract class E2ETestBase {

    protected static final String ADMIN_BASE_URL = System.getProperty("admin.baseUrl", "http://localhost:8081");
    protected static final String SURVEY_BASE_URL = System.getProperty("survey.baseUrl", "http://localhost:8080");
    protected static final String ADMIN_USERNAME = System.getProperty("admin.username", "admin");
    protected static final String ADMIN_PASSWORD = System.getProperty("admin.password", "admin");
    protected static final String AUTHOR_BASE_URL = System.getProperty("author.baseUrl", "http://localhost:8084");
    protected static final String AUTHOR_USERNAME = System.getProperty("author.username", "author");
    protected static final String AUTHOR_PASSWORD = System.getProperty("author.password", "author");
    /** The survey imported into the stack under test (Family History Survey, survey id 1). */
    protected static final String SEEDED_SURVEY_NAME = System.getProperty("seeded.survey.name", "Family History Survey");
    /**
     * The department this suite works in. Nothing seeds one any more (Admin UC-028 C-016), so
     * the suite creates it on the first login that finds the blocking dialog. The name is the
     * one the migration used to seed, which keeps the search filter's "All departments" wording
     * and the CSV fixture's department id 1 meaningful on a reset stack.
     */
    protected static final String ADMIN_DEPARTMENT = System.getProperty("admin.department", "Testing Department");
    protected static final String ADMIN_DEPARTMENT_CODE = System.getProperty("admin.department.code", "Test");
    protected static final String ADMIN_DEPARTMENT_EMAIL =
            System.getProperty("admin.department.email", "test@testdepartment.org");

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

    /**
     * Throws away the current browser context (cookies, Keycloak SSO session and all) and starts
     * a fresh one, so the test can sign in to a second app as a different Keycloak user. All
     * three apps share one Keycloak realm: without this, a browser that is still signed in as
     * {@code author} would be silently single-signed-on into Admin as {@code author} -- who has
     * no {@code elicit_admin} role -- instead of being shown the login form.
     */
    protected void resetBrowserContext() {
        context.close();
        createPage();
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

    /**
     * Gives the signed-in administrator a department to work in (Admin UC-028).
     * <p>
     * A fresh database seeds none, so the console blocks every screen with a modal dialog until
     * one exists. This follows the dialog's own remedy: click through to Departments and create
     * the department, which Admin assigns to its creator. Call it straight after signing in and
     * before any register, search or apply step.
     * <p>
     * Idempotent, and cheap when there is nothing to do: on a stack that already has the
     * department (a rerun, or another test class in the same suite got there first) no dialog
     * appears and this returns at once.
     */
    protected void bootstrapDepartment() {
        // The dialog is detected by a button in its footer: Vaadin renders a Dialog's contents
        // into an overlay element, so the id set on the Dialog itself is not what the page shows.
        if (!isBlockingDepartmentDialogOpen(page)) {
            return;
        }
        page.locator("#missing-department-add").click();
        page.waitForURL(url -> url.endsWith("/departments"));
        if (page.getByText(ADMIN_DEPARTMENT, new Page.GetByTextOptions().setExact(true)).count() > 0) {
            return; // another test class created it while this one was signing in
        }
        openAdmin("/edit-department/0");
        new DepartmentsPage(page).createDepartment(ADMIN_DEPARTMENT, ADMIN_DEPARTMENT_CODE, ADMIN_DEPARTMENT_EMAIL);
    }

    protected void openSurvey(String path) {
        page.navigate(SURVEY_BASE_URL + path);
    }

    protected void openAuthor(String path) {
        page.navigate(AUTHOR_BASE_URL + path);
    }

    /** Whether the no-department dialog (Admin UC-028) is on screen, by its Logout button. */
    protected static boolean isBlockingDepartmentDialogOpen(Page page) {
        Locator logout = page.locator("#missing-department-logout");
        return logout.count() > 0 && logout.first().isVisible();
    }

    /** What the PDF endpoint really answered when a report popup requested it. */
    public record PdfDownload(String url, String contentType, int size) {}

    /**
     * Runs {@code action}, which must open a report PDF in a new tab, and returns what the PDF
     * endpoint answered. Both apps' PDF flows do {@code window.open('/api/pdf/download?key=…',
     * '_blank')}; headless Chromium has no PDF viewer, so it turns that response into a file
     * download and abandons the popup's navigation -- leaving the tab on {@code about:blank}
     * with no load event, which made asserting on the popup's URL a race (seen failing live).
     *
     * <p>So instead a context-wide route intercepts the popup's request, fetches the real
     * response from the server (content type and size are what the caller asserts on), and
     * fulfils the popup with a trivial HTML page so it loads normally and can be closed.</p>
     */
    protected PdfDownload openPdfPopup(Runnable action) {
        String pattern = "**/api/pdf/download*";
        AtomicReference<PdfDownload> captured = new AtomicReference<>();
        context.route(pattern, route -> {
            APIResponse real = route.fetch();
            captured.set(new PdfDownload(route.request().url(),
                    real.headers().get("content-type"), real.body().length));
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("text/html")
                    .setBody("<html><body>PDF response captured by the e2e suite</body></html>"));
        });
        try {
            Page popup = page.waitForPopup(action::run);
            popup.waitForLoadState();
            popup.close();
        } finally {
            context.unroute(pattern);
        }
        PdfDownload download = captured.get();
        if (download == null) {
            throw new IllegalStateException("The popup never requested /api/pdf/download");
        }
        return download;
    }
}
