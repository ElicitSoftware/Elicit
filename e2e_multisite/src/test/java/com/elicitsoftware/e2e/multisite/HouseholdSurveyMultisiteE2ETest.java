package com.elicitsoftware.e2e.multisite;

import com.elicitsoftware.e2e.admin.DepartmentsPage;
import com.elicitsoftware.e2e.admin.EditUserPage;
import com.elicitsoftware.e2e.admin.KeycloakLoginHelper;
import com.elicitsoftware.e2e.admin.RegisterPage;
import com.elicitsoftware.e2e.admin.RespondentImportPage;
import com.elicitsoftware.e2e.admin.SearchPage;
import com.elicitsoftware.e2e.admin.SurveyApplyPage;
import com.elicitsoftware.e2e.author.DesignerPage;
import com.elicitsoftware.e2e.author.SurveyEditorPage;
import com.elicitsoftware.e2e.author.SurveysPage;
import com.elicitsoftware.e2e.survey.LoginPage;
import com.elicitsoftware.e2e.survey.ReviewPage;
import com.elicitsoftware.e2e.survey.SectionPage;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.elicitsoftware.e2e.multisite.HouseholdSurvey.PEOPLE;
import static com.elicitsoftware.e2e.multisite.HouseholdSurvey.SEX_OPTIONS_V1;
import static com.elicitsoftware.e2e.multisite.HouseholdSurvey.SEX_OPTIONS_V2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The multi-site theory, end to end: site 1 (master) authors the Household Survey and applies
 * it; site 2 applies the same {@code .elicit} file later; respondents on both sites start,
 * pause and finish across a revision that site 1 applies before site 2 does; then site 2's
 * respondents are exported one by one and imported into site 1, where they must arrive with
 * their status, department and answers intact.
 *
 * <p>One ordered story, one phase per test method. A failed phase marks the rest as skipped
 * ("theory broke at phase N"), so the surefire report shows where the story stopped. Every
 * persona visit runs in a fresh browser context. The survey is always the one Household Survey
 * (one survey key across both revisions and both sites), so the databases must be cleared
 * between runs (./reset.sh all, or ./run.sh which resets, starts and tests); phase 1 refuses
 * to start otherwise.</p>
 *
 * <p>Traceability: Author UC-001, UC-003, UC-004, UC-006, UC-007, UC-008, UC-011, UC-014,
 * UC-015, UC-016, UC-019. Admin UC-001, UC-002, UC-003, UC-011, UC-012, UC-017, UC-018, the
 * departments and users views. Survey UC-001 to UC-006 (answering with REPEAT and SHOW rules,
 * resuming, finishing).</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HouseholdSurveyMultisiteE2ETest extends MultisiteTestBase {

    private static final Pattern SURVEY_KEY_LINE = Pattern.compile("(?m)^# survey_key: (\\S+)$");
    private static final String SITE2_DEPARTMENT = "Site 2 Clinic";
    private static final String SITE2_DEPARTMENT_CODE = "SITE2";
    private static final String SITE1_DEPARTMENT = "Testing Department";

    /** Distinguishes this run's respondents; the survey itself is always the one Household Survey. */
    private final String runId = String.valueOf(System.nanoTime());
    private final String surveyName = HouseholdSurvey.NAME;
    private Path exportDir;
    private String surveyKey;
    private Path v1File;
    private Path v2File;
    /** Access codes by respondent label (r1a, r1b, r1c, r2a, r2b, r2c, r2d). */
    private final Map<String, String> codes = new LinkedHashMap<>();
    /** Site 2 export files by respondent label. */
    private final Map<String, Path> site2Exports = new LinkedHashMap<>();
    private String failedPhase;

    // ---- phase plumbing ----------------------------------------------------------------------

    private void phase(String name, PhaseBody body) {
        assumeTrue(failedPhase == null, "skipped: the story already broke at " + failedPhase);
        try {
            body.run();
        } catch (Throwable t) {
            failedPhase = name;
            if (t instanceof RuntimeException re) {
                throw re;
            }
            if (t instanceof Error e) {
                throw e;
            }
            throw new RuntimeException(t);
        }
    }

    @FunctionalInterface
    private interface PhaseBody {
        void run() throws Exception;
    }

    private Path exportDir() throws IOException {
        if (exportDir == null) {
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            exportDir = Files.createDirectories(Path.of("target", "exports", stamp));
        }
        return exportDir;
    }

    private static void adminLogin(Page page, Site site) {
        page.navigate(site.adminBaseUrl() + "/");
        new KeycloakLoginHelper(page).login(ADMIN_USERNAME, ADMIN_PASSWORD);
        page.waitForURL(url -> url.startsWith(site.adminBaseUrl()));
    }

    private static void authorLogin(Page page, Site site) {
        openAuthor(page, site, "/");
        new KeycloakLoginHelper(page).login(AUTHOR_USERNAME, AUTHOR_PASSWORD);
        page.waitForURL(url -> url.startsWith(site.authorBaseUrl()));
    }

    // ---- phases -----------------------------------------------------------------------------

    @Test
    @Order(1)
    void phase01_site1AuthorsAndExportsRevision1() {
        phase("1 author v1", () -> visit(page -> {
            authorLogin(page, SITE1);
            SurveysPage surveys = new SurveysPage(page);
            surveys.waitUntilLoaded();
            assertTrue(!surveys.hasSurvey(surveyName), "'" + surveyName + "' already exists in Author: the databases were not"
                    + " cleared since the last run. Run ./reset.sh all (or ./run.sh) first.");
            surveys.createSurvey(surveyName, HouseholdSurvey.TITLE);
            int surveyId = surveys.openSurvey(surveyName);

            openAuthor(page, SITE1, "/survey/" + surveyId + "/design");
            DesignerPage designer = new DesignerPage(page);
            designer.addStep(HouseholdSurvey.STEP1);
            designer.addSection(HouseholdSurvey.STEP1, HouseholdSurvey.STEP1_SECTION);
            designer.addQuestion(HouseholdSurvey.STEP1, HouseholdSurvey.STEP1_SECTION, HouseholdSurvey.Q_COUNT, "INTEGER", 1, 10);
            designer.addQuestion(HouseholdSurvey.STEP1, HouseholdSurvey.STEP1_SECTION, HouseholdSurvey.Q_NAME, "TEXT", null, null);
            designer.addStep(HouseholdSurvey.STEP2);
            designer.addSection(HouseholdSurvey.STEP2, HouseholdSurvey.STEP2_SECTION);
            designer.addQuestion(HouseholdSurvey.STEP2, HouseholdSurvey.STEP2_SECTION, HouseholdSurvey.Q_AGE, "INTEGER", 0, 120);
            designer.addChoiceQuestion(HouseholdSurvey.STEP2, HouseholdSurvey.STEP2_SECTION, HouseholdSurvey.Q_SEX_V1,
                    HouseholdSurvey.SEX_LIST, SEX_OPTIONS_V1);
            designer.addQuestion(HouseholdSurvey.STEP2, HouseholdSurvey.STEP2_SECTION, HouseholdSurvey.Q_RELATION, "TEXT", null, null);
            designer.addQuestion(HouseholdSurvey.STEP2, HouseholdSurvey.STEP2_SECTION, HouseholdSurvey.Q_CHILDREN, "INTEGER", 0, 30);
            // Rule 1: the count repeats the name question once per person.
            designer.addRule(HouseholdSurvey.Q_COUNT, "GREATER THAN", "0", "REPEAT", null, "Question", HouseholdSurvey.Q_NAME);
            // Rule 2: each name shows Step 2 once, carrying the name as token NAME.
            designer.addRule(HouseholdSurvey.Q_NAME, "FIELD_EXIST", null, "SHOW", "NAME", "Step", HouseholdSurvey.STEP2);

            openAuthor(page, SITE1, "/survey/" + surveyId);
            SurveyEditorPage editor = new SurveyEditorPage(page);
            editor.setInitialDisplayKey(SurveyEditorPage.firstSectionDisplayKey(surveyId));
            v1File = editor.export("Household v1", exportDir());
            String definition = Files.readString(v1File, StandardCharsets.UTF_8);
            assertTrue(definition.startsWith("# ELICIT_SURVEY_EXPORT_V1"), "unexpected export header");
            assertTrue(definition.contains("# survey_name: " + surveyName), "export should carry the survey name");
            Matcher m = SURVEY_KEY_LINE.matcher(definition);
            assertTrue(m.find(), "export should carry the survey key");
            surveyKey = m.group(1);
            assertTrue(definition.contains("# relationships: 2"), "export should carry both rules:\n" + headerOf(definition));
        }));
    }

    @Test
    @Order(2)
    void phase02_site1AppliesRevision1() {
        phase("2 site 1 apply v1", () -> visit(page -> {
            adminLogin(page, SITE1);
            openAdmin(page, SITE1, "/survey-apply");
            String result = new SurveyApplyPage(page).apply(v1File);
            assertTrue(result.contains("New Survey Installed"), "unexpected apply outcome on site 1:\n" + result);
            assertTrue(result.contains(surveyKey), "apply dialog should echo the survey key:\n" + result);
            assertReportingRebuilt(result, SITE1);
        }));
    }

    @Test
    @Order(3)
    void phase03_site2CreatesItsDepartmentAndAppliesRevision1() {
        phase("3 site 2 department + apply v1", () -> visit(page -> {
            adminLogin(page, SITE2);
            ensureDepartment(page, SITE2);
            openAdmin(page, SITE2, "/edit-user/1");
            new EditUserPage(page).addDepartment(SITE2_DEPARTMENT);
            openAdmin(page, SITE2, "/survey-apply");
            String result = new SurveyApplyPage(page).apply(v1File);
            assertTrue(result.contains("New Survey Installed"), "unexpected apply outcome on site 2:\n" + result);
            assertReportingRebuilt(result, SITE2);
        }));
    }

    @Test
    @Order(4)
    void phase04_site1RespondentsOnRevision1() {
        phase("4 site 1 respondents on v1", () -> {
            register(SITE1, SITE1_DEPARTMENT, "r1a", "r1b", "r1c");
            takeWholeSurvey(SITE1, "r1a", "sex", SEX_OPTIONS_V1);
            startAndPause(SITE1, "r1b", "sex");
            assertStatus(SITE1, Map.of("r1a", "Finished", "r1b", "In Progress", "r1c", "Not Started"));
        });
    }

    @Test
    @Order(5)
    void phase05_site2RespondentsOnRevision1() {
        phase("5 site 2 respondents on v1", () -> {
            register(SITE2, SITE2_DEPARTMENT, "r2a", "r2b", "r2c");
            takeWholeSurvey(SITE2, "r2a", "sex", SEX_OPTIONS_V1);
            startAndPause(SITE2, "r2b", "sex");
            assertStatus(SITE2, Map.of("r2a", "Finished", "r2b", "In Progress", "r2c", "Not Started"));
        });
    }

    @Test
    @Order(6)
    void phase06_site1AuthorsAndAppliesRevision2() {
        phase("6 site 1 v2", () -> {
            visit(page -> {
                authorLogin(page, SITE1);
                SurveysPage surveys = new SurveysPage(page);
                surveys.waitUntilLoaded();
                int surveyId = surveys.openSurvey(surveyName);
                openAuthor(page, SITE1, "/survey/" + surveyId + "/design");
                DesignerPage designer = new DesignerPage(page);
                designer.editQuestion(HouseholdSurvey.STEP2, HouseholdSurvey.STEP2_SECTION, HouseholdSurvey.Q_SEX_V1, HouseholdSurvey.Q_SEX_V2);
                designer.editListOptions(HouseholdSurvey.SEX_LIST, SEX_OPTIONS_V2);
                openAuthor(page, SITE1, "/survey/" + surveyId);
                v2File = new SurveyEditorPage(page).export("Sex -> Gender", exportDir());
                String definition = Files.readString(v2File, StandardCharsets.UTF_8);
                assertTrue(definition.contains("gender?"), "v2 export should carry the gender wording");
                assertTrue(definition.contains("Woman"), "v2 export should carry the new options");
            });
            visit(page -> {
                adminLogin(page, SITE1);
                openAdmin(page, SITE1, "/survey-apply");
                String result = new SurveyApplyPage(page).apply(v2File);
                assertTrue(result.contains("Survey Updated"), "unexpected apply outcome for v2 on site 1:\n" + result);
                assertReportingRebuilt(result, SITE1);
            });
        });
    }

    @Test
    @Order(7)
    void phase07_site2StillOnRevision1RegistersAndFinishesOne() {
        phase("7 site 2 gap respondent", () -> {
            register(SITE2, SITE2_DEPARTMENT, "r2d");
            takeWholeSurvey(SITE2, "r2d", "sex", SEX_OPTIONS_V1);
            assertStatus(SITE2, Map.of("r2d", "Finished"));
        });
    }

    @Test
    @Order(8)
    void phase08_site1InFlightAndNewRespondentsAfterRevision2() {
        phase("8 site 1 after v2", () -> {
            resumeAndFinish(SITE1, "r1b", "sex", SEX_OPTIONS_V1);
            takeWholeSurvey(SITE1, "r1c", "gender", SEX_OPTIONS_V2);
            assertStatus(SITE1, Map.of("r1b", "Finished", "r1c", "Finished"));
        });
    }

    @Test
    @Order(9)
    void phase09_site2AppliesRevision2() {
        phase("9 site 2 apply v2", () -> visit(page -> {
            adminLogin(page, SITE2);
            openAdmin(page, SITE2, "/survey-apply");
            String result = new SurveyApplyPage(page).apply(v2File);
            assertTrue(result.contains("Survey Updated"), "unexpected apply outcome for v2 on site 2:\n" + result);
            assertReportingRebuilt(result, SITE2);
        }));
    }

    @Test
    @Order(10)
    void phase10_site2InFlightAndNewRespondentsAfterRevision2() {
        phase("10 site 2 after v2", () -> {
            resumeAndFinish(SITE2, "r2b", "sex", SEX_OPTIONS_V1);
            takeWholeSurvey(SITE2, "r2c", "gender", SEX_OPTIONS_V2);
            assertStatus(SITE2, Map.of("r2b", "Finished", "r2c", "Finished"));
        });
    }

    @Test
    @Order(11)
    void phase11_site2ExportsItsRespondents() {
        phase("11 site 2 export", () -> visit(page -> {
            adminLogin(page, SITE2);
            openAdmin(page, SITE2, "/");
            SearchPage search = new SearchPage(page);
            for (String label : List.of("r2a", "r2b", "r2c", "r2d")) {
                search.searchByAccessCode(codes.get(label));
                assertEquals(1, search.rowCount(), "expected one row for " + label);
                Path file = search.exportRespondent(codes.get(label), exportDir());
                String text = Files.readString(file, StandardCharsets.UTF_8);
                assertTrue(text.startsWith("# ELICIT_EXPORT_V2"), label + ": unexpected export header:\n" + headerOf(text));
                assertTrue(text.contains("# survey_key: " + surveyKey), label + ": export should name the survey by key");
                site2Exports.put(label, file);
            }
        }));
    }

    @Test
    @Order(12)
    void phase12_site1ImportsSite2Respondents() {
        phase("12 site 1 import", () -> visit(page -> {
            adminLogin(page, SITE1);
            ensureDepartment(page, SITE1);
            // Search only lists the signed-in user's departments, so the master site's admin must
            // belong to the site 2 department to see (and later report on) the imported respondents.
            openAdmin(page, SITE1, "/edit-user/1");
            new EditUserPage(page).addDepartment(SITE2_DEPARTMENT);
            RespondentImportPage importer = new RespondentImportPage(page);
            for (Map.Entry<String, Path> e : site2Exports.entrySet()) {
                // A fresh view per file: the upload component accepts a single file per instance.
                openAdmin(page, SITE1, "/respondent-import");
                String result = importer.importFile(e.getValue());
                assertTrue(result.contains("Import Successful"), e.getKey() + ": unexpected import outcome:\n" + result);
            }
        }));
    }

    @Test
    @Order(13)
    void phase13_site1HoldsEveryRespondent() {
        phase("13 site 1 consolidated", () -> visit(page -> {
            adminLogin(page, SITE1);
            openAdmin(page, SITE1, "/");
            SearchPage search = new SearchPage(page);
            Map<String, String> expectedStatus = Map.of(
                    "r1a", "Finished", "r1b", "Finished", "r1c", "Finished",
                    "r2a", "Finished", "r2b", "Finished", "r2c", "Finished", "r2d", "Finished");
            List<String> problems = new ArrayList<>();
            for (Map.Entry<String, String> e : expectedStatus.entrySet()) {
                search.searchByAccessCode(codes.get(e.getKey()));
                if (search.rowCount() != 1) {
                    problems.add(e.getKey() + ": expected 1 row, found " + search.rowCount());
                    continue;
                }
                if (!e.getValue().equals(search.statusAt(0))) {
                    problems.add(e.getKey() + ": status " + search.statusAt(0) + ", expected " + e.getValue());
                }
                String expectedDept = e.getKey().startsWith("r2") ? SITE2_DEPARTMENT : SITE1_DEPARTMENT;
                if (!expectedDept.equals(search.departmentAt(0))) {
                    problems.add(e.getKey() + ": department " + search.departmentAt(0) + ", expected " + expectedDept);
                }
            }
            assertTrue(problems.isEmpty(), "site 1 after import:\n" + String.join("\n", problems));

            // Round trip: re-exporting an imported respondent from site 1 must reproduce site 2's
            // answer and dependent lines (V2 carries no site-local ids in them).
            for (String label : List.of("r2a", "r2b")) {
                search.searchByAccessCode(codes.get(label));
                Path again = search.exportRespondent(codes.get(label), Files.createDirectories(exportDir().resolve("site1-reexport")));
                List<String> original = dataLines(Files.readString(site2Exports.get(label), StandardCharsets.UTF_8));
                List<String> reexported = dataLines(Files.readString(again, StandardCharsets.UTF_8));
                assertEquals(original, reexported, label + ": answers/dependents differ after the import round trip");
            }
        }));
    }

    // ---- respondent journeys ------------------------------------------------------------------

    private void register(Site site, String department, String... labels) {
        visit(page -> {
            adminLogin(page, site);
            for (String label : labels) {
                openAdmin(page, site, "/register");
                String first = "Multi";
                String last = label.toUpperCase() + runId;
                new RegisterPage(page).registerSubject(surveyName, department, first, last, label + "." + runId + "@example.org");
                openAdmin(page, site, "/");
                SearchPage search = new SearchPage(page);
                search.searchByFirstAndLastName(first, last);
                assertEquals(1, search.rowCount(), "expected exactly one row for " + label + " on " + site);
                assertEquals("Not Started", search.statusAt(0));
                codes.put(label, search.accessCodeAt(0));
            }
        });
    }

    private void login(Page page, Site site, String label) {
        openSurvey(page, site, "/login/" + codes.get(label));
        new LoginPage(page).loginWithAccessCode(surveyName, codes.get(label));
        assertTrue(page.url().contains("/section"), label + " should land on a section, was " + page.url());
    }

    /** Logs in, answers Step 1 and both Step 2 instances, finishes. */
    private void takeWholeSurvey(Site site, String label, String sexWord, List<String> options) {
        visit(page -> {
            login(page, site, label);
            SectionPage section = new SectionPage(page);
            HouseholdSurvey.answerStep1(section);
            section.next();
            finishMembers(section, sexWord, options);
            openSurvey(page, site, "/logout");
        });
    }

    /** Logs in, completes Step 1 (which creates the two Step 2 instances), peeks at the first member, leaves. */
    private void startAndPause(Site site, String label, String sexWord) {
        visit(page -> {
            login(page, site, label);
            SectionPage section = new SectionPage(page);
            HouseholdSurvey.answerStep1(section);
            section.next();
            HouseholdSurvey.assertOnMember(section, 1);
            String sexLabel = section.labelOf(section.keyEndingWith(HouseholdSurvey.memberSuffix(1, 2)));
            assertTrue(sexLabel.toLowerCase().contains(sexWord), label + ": expected '" + sexWord + "' in: " + sexLabel);
            openSurvey(page, site, "/logout");
        });
    }

    /** Logs back in, walks forward to the first member's step (Step 1 answers must still be there), finishes. */
    private void resumeAndFinish(Site site, String label, String sexWord, List<String> options) {
        visit(page -> {
            login(page, site, label);
            SectionPage section = new SectionPage(page);
            if (section.hasKeyEndingWith(HouseholdSurvey.countSuffix())) {
                assertEquals(String.valueOf(PEOPLE.size()), section.valueOf(section.keyEndingWith(HouseholdSurvey.countSuffix())),
                        label + ": the household count should have been kept");
                assertEquals(PEOPLE.get(0).name(), section.valueOf(section.keyEndingWith(HouseholdSurvey.nameSuffix(1))),
                        label + ": the first name should have been kept");
                section.next();
            }
            for (int i = 0; i < 4 && !HouseholdSurvey.onMember(section, 1); i++) {
                section.next();
            }
            finishMembers(section, sexWord, options);
            openSurvey(page, site, "/logout");
        });
    }

    private void finishMembers(SectionPage section, String sexWord, List<String> options) {
        for (int person = 1; person <= PEOPLE.size(); person++) {
            HouseholdSurvey.answerMember(section, person, sexWord, options);
            if (person < PEOPLE.size()) {
                section.next();
            } else {
                section.review();
            }
        }
        ReviewPage review = new ReviewPage(section.page());
        List<String> titles = review.sectionTitles();
        for (int person = 1; person <= PEOPLE.size(); person++) {
            assertTrue(titles.contains(HouseholdSurvey.reviewTitle(person)),
                    "review should list " + HouseholdSurvey.reviewTitle(person) + " but lists " + titles);
        }
        review.finish();
    }

    private void assertStatus(Site site, Map<String, String> expected) {
        visit(page -> {
            adminLogin(page, site);
            openAdmin(page, site, "/");
            SearchPage search = new SearchPage(page);
            List<String> problems = new ArrayList<>();
            for (Map.Entry<String, String> e : expected.entrySet()) {
                search.searchByAccessCode(codes.get(e.getKey()));
                String actual = search.rowCount() == 1 ? search.statusAt(0) : "(" + search.rowCount() + " rows)";
                if (!e.getValue().equals(actual)) {
                    problems.add(e.getKey() + ": " + actual + ", expected " + e.getValue());
                }
            }
            if (!problems.isEmpty()) {
                fail("status on " + site + ":\n" + String.join("\n", problems));
            }
        });
    }

    private void ensureDepartment(Page page, Site site) {
        openAdmin(page, site, "/departments");
        // The grid renders after the view's fixed "New Department" button; give it a moment
        // before deciding the department is missing (a rerun on a live stack finds it here).
        page.locator("vaadin-button").filter(new com.microsoft.playwright.Locator.FilterOptions().setHasText("New Department")).first().waitFor();
        page.waitForTimeout(1000);
        if (page.getByText(SITE2_DEPARTMENT, new Page.GetByTextOptions().setExact(true)).count() > 0) {
            return;
        }
        openAdmin(page, site, "/edit-department/0");
        new DepartmentsPage(page).createDepartment(SITE2_DEPARTMENT, SITE2_DEPARTMENT_CODE, "site2@example.org");
    }

    /** Admin UC-018 BR-107: a successful apply asks Survey to rebuild the reporting star schema. */
    private static void assertReportingRebuilt(String applyResult, Site site) {
        assertTrue(applyResult.contains("Reporting schema rebuilt."),
                site + ": the apply should have rebuilt the reporting schema:\n" + applyResult);
    }

    private static String headerOf(String text) {
        return text.lines().filter(l -> l.startsWith("#")).collect(Collectors.joining("\n"));
    }

    /** The {@code answers:} and {@code dependents:} lines of a respondent export, in file order. */
    private static List<String> dataLines(String text) {
        return text.lines().filter(l -> l.startsWith("answers: ") || l.startsWith("dependents: ")).toList();
    }
}
