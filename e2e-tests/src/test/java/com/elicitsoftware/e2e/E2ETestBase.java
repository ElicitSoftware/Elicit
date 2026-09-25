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
import com.elicitsoftware.e2e.admin.KeycloakLoginHelper;
import com.elicitsoftware.e2e.admin.SurveyApplyPage;
import com.microsoft.playwright.TimeoutError;

import java.nio.file.Files;
import java.nio.file.Path;
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
     * The definition {@link #ensureSeededSurveyInstalled()} applies when that survey is not on the
     * stack yet. Relative paths resolve against this module's directory (surefire's working
     * directory), so the default reaches the copy FHHS ships in the umbrella checkout.
     */
    protected static final String SEEDED_SURVEY_FILE =
            System.getProperty("seeded.survey.file", "../FHHS/family-history-survey.elicit");
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
    /** Guards {@link #ensureSeededSurveyInstalled()}: once per JVM, not once per test class. */
    private static boolean seededSurveyChecked;

    protected BrowserContext context;
    protected Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        boolean headless = !"false".equals(System.getProperty("e2e.headless"));
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setArgs(List.of("--window-size=1440,1024")));
        ensureSeededSurveyInstalled();
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
        bootstrapDepartment(page);
    }

    /** {@link #bootstrapDepartment()} on an arbitrary page -- used by the static bootstrap below. */
    protected static void bootstrapDepartment(Page page) {
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
        page.navigate(ADMIN_BASE_URL + "/edit-department/0");
        new DepartmentsPage(page).createDepartment(ADMIN_DEPARTMENT, ADMIN_DEPARTMENT_CODE, ADMIN_DEPARTMENT_EMAIL);
    }

    protected void openSurvey(String path) {
        page.navigate(SURVEY_BASE_URL + path);
    }

    protected void openAuthor(String path) {
        page.navigate(AUTHOR_BASE_URL + path);
    }

    /**
     * Installs {@link #SEEDED_SURVEY_NAME} on the stack under test if it is not there already
     * (Admin UC-018), once per JVM, before any test runs.
     *
     * <p>Nothing seeds a survey any more, but {@link RespondentJourneyE2ETest},
     * {@link SearchFiltersE2ETest} and {@link RegisterViaCsvE2ETest} all register against this
     * one -- so on a greenfield stack ({@code ../resetDatabase.sh V3}) the suite used to need a
     * manual "Apply Survey Definition" in the console first. This does that step itself, from the
     * definition FHHS ships ({@link #SEEDED_SURVEY_FILE}), which is also what the operator is told
     * to upload (DeploymentScript.md). {@link AuthorToRespondentE2ETest} authors and applies its
     * own survey and does not depend on this.</p>
     *
     * <p>It is a no-op when the survey is already installed, so a brownfield stack (V2 upgrade) and
     * a second run against the same stack are left exactly as they were -- the apply is skipped
     * rather than re-run as an update. Work happens in its own browser context so it cannot leak an
     * Admin SSO session into the test that triggered it.</p>
     */
    private static void ensureSeededSurveyInstalled() {
        if (seededSurveyChecked) {
            return;
        }
        seededSurveyChecked = true;
        Path definition = Path.of(SEEDED_SURVEY_FILE).toAbsolutePath().normalize();
        BrowserContext bootstrapContext = browser.newContext(new Browser.NewContextOptions().setViewportSize(1440, 1024));
        try (bootstrapContext) {
            Page bootstrapPage = bootstrapContext.newPage();
            // Admin restores the requested path after the OIDC redirect
            // (quarkus.oidc.authentication.restore-path-after-redirect), so one navigation and one
            // login land on /register -- no waiting on intermediate callback URLs.
            bootstrapPage.navigate(ADMIN_BASE_URL + "/register");
            new KeycloakLoginHelper(bootstrapPage).login(ADMIN_USERNAME, ADMIN_PASSWORD);
            waitForRegisterView(bootstrapPage);
            if (isBlockingDepartmentDialogOpen(bootstrapPage)) {
                // Admin UC-028: the modal makes every other component inert -- a Vaadin Upload
                // under it answers 403 -- so the department has to exist before the apply.
                bootstrapDepartment(bootstrapPage);
                bootstrapPage.navigate(ADMIN_BASE_URL + "/register");
                waitForRegisterView(bootstrapPage);
            }
            if (isSeededSurveyInstalled(bootstrapPage)) {
                return;
            }
            if (!Files.isReadable(definition)) {
                throw new IllegalStateException(SEEDED_SURVEY_NAME + " is not installed on "
                        + ADMIN_BASE_URL + " and its definition is not readable at " + definition
                        + " -- set -Dseeded.survey.file=... or apply it in the console first.");
            }
            bootstrapPage.navigate(ADMIN_BASE_URL + "/survey-apply");
            String outcome = new SurveyApplyPage(bootstrapPage).apply(definition);
            if (!outcome.contains("New Survey Installed") && !outcome.contains("Survey Updated")) {
                throw new IllegalStateException("Could not install " + SEEDED_SURVEY_NAME
                        + " from " + definition + "; Admin answered:\n" + outcome);
            }
        }
    }

    /**
     * Waits for RegisterView to be usable, or for the no-department dialog that covers it. The
     * Save button is unconditional; the survey selector is not (it has nothing to offer on a stack
     * with no survey), so it is the wrong thing to wait on here.
     */
    private static void waitForRegisterView(Page page) {
        page.locator("[id=\"register-save-button\"], #missing-department-logout").first().waitFor();
    }

    /** Whether RegisterView's survey selector offers {@link #SEEDED_SURVEY_NAME}. */
    private static boolean isSeededSurveyInstalled(Page page) {
        Locator selector = page.locator("[id=\"register-survey\"] input");
        if (selector.count() == 0) {
            return false;
        }
        selector.click();
        try {
            page.locator("vaadin-combo-box-item")
                    .filter(new Locator.FilterOptions().setHasText(SEEDED_SURVEY_NAME)).first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(3000));
            return true;
        } catch (TimeoutError notInstalled) {
            return false;
        } finally {
            page.keyboard().press("Escape");
        }
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
