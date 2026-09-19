package com.elicitsoftware.e2e;

import com.elicitsoftware.e2e.admin.KeycloakLoginHelper;
import com.elicitsoftware.e2e.admin.RegisterPage;
import com.elicitsoftware.e2e.admin.SearchPage;
import com.elicitsoftware.e2e.admin.SurveyApplyPage;
import com.elicitsoftware.e2e.author.DesignerPage;
import com.elicitsoftware.e2e.author.SurveyEditorPage;
import com.elicitsoftware.e2e.author.SurveysPage;
import com.elicitsoftware.e2e.survey.LoginPage;
import com.elicitsoftware.e2e.survey.ReportPage;
import com.elicitsoftware.e2e.survey.ReviewPage;
import com.elicitsoftware.e2e.survey.SectionPage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The full authoring-to-respondent story across all three apps: an author designs a brand-new
 * survey in Author and exports it as a {@code .elicit} file; an administrator applies that file
 * in Admin and registers a respondent against the new survey; the respondent enters Survey with
 * the generated access code, answers, finishes and downloads the report; and Admin shows them
 * as Finished.
 *
 * <p>The survey is the smallest one that can be exported and completed: one step, one section,
 * one required-free TEXT question, no reports, no rules. Every run creates a survey with a
 * unique name (Author's and Admin's survey names are unique), so the test is repeatable against
 * the same stack; it leaves those surveys behind, since neither app offers a delete.</p>
 *
 * <p>Traceability: Author UC-001 (sign in), UC-003 (create survey), UC-006 (open), UC-016 (add
 * step), UC-015 (add section), UC-011 (add question), UC-004 (edit details -- initial display
 * key), UC-007 (validation), UC-008 (export). Admin UC-001, UC-018 (apply survey definition),
 * UC-003 (register a subject, on the newly applied survey), UC-002 (search, twice). Survey
 * UC-001 (enter via access code, choosing the survey), UC-002, UC-003, UC-004, UC-005, UC-006.</p>
 */
class AuthorToRespondentE2ETest extends E2ETestBase {

    private static final Pattern SURVEY_KEY_LINE = Pattern.compile("(?m)^# survey_key: (\\S+)$");

    @Test
    void authoredSurveyIsAppliedRegisteredAndTaken() throws IOException {
        String uniqueSuffix = String.valueOf(System.nanoTime());
        String surveyName = "E2E Survey " + uniqueSuffix;
        String surveyTitle = "E2E Survey";

        // --- Author -------------------------------------------------------------------------
        // 1. Author UC-001: sign in through Keycloak as the author user (client role elicit_author).
        openAuthor("/");
        new KeycloakLoginHelper(page).login(AUTHOR_USERNAME, AUTHOR_PASSWORD);
        SurveysPage surveysPage = new SurveysPage(page);
        surveysPage.waitUntilLoaded();

        // 2. Author UC-003 / UC-006: create the survey and open it to learn its id.
        surveysPage.createSurvey(surveyName, surveyTitle);
        int authorSurveyId = surveysPage.openSurvey(surveyName);

        // 3. Author UC-016 / UC-015 / UC-011: one step, one section, one text question.
        openAuthor("/survey/" + authorSurveyId + "/design");
        DesignerPage designer = new DesignerPage(page);
        designer.addStep("Welcome");
        designer.addSectionToFirstStep("Basics");
        designer.addTextQuestionToFirstSection("What is your favourite colour?");

        // 4. Author UC-004 / UC-007: point the survey at its first section, then expect a clean
        //    validation panel -- Export is disabled while any error remains.
        openAuthor("/survey/" + authorSurveyId);
        SurveyEditorPage editor = new SurveyEditorPage(page);
        editor.setInitialDisplayKey(SurveyEditorPage.firstSectionDisplayKey(authorSurveyId));
        assertTrue(editor.isReadyToExport(), "expected no validation findings but got:\n" + editor.validationText());

        // 5. Author UC-008: export the definition and keep the downloaded .elicit file.
        Path downloadDir = Files.createTempDirectory("elicit-e2e-export");
        Path elicitFile = editor.export("E2E initial publication", downloadDir);
        assertTrue(elicitFile.getFileName().toString().endsWith(".elicit"), elicitFile.toString());
        String definition = Files.readString(elicitFile, StandardCharsets.UTF_8);
        assertTrue(definition.startsWith("# ELICIT_SURVEY_EXPORT_V1"), "unexpected export header");
        assertTrue(definition.contains("# survey_name: " + surveyName), "export should carry the survey name");
        assertTrue(definition.contains("# questions: 1"), "export should carry the single question");
        Matcher keyMatcher = SURVEY_KEY_LINE.matcher(definition);
        assertTrue(keyMatcher.find(), "export should carry the survey key");
        String surveyKey = keyMatcher.group(1);

        // --- Admin --------------------------------------------------------------------------
        // A fresh browser context: the author's Keycloak SSO session must not leak into Admin.
        resetBrowserContext();

        // 6. Admin UC-001: sign in as the administrator.
        openAdmin("/");
        new KeycloakLoginHelper(page).login(ADMIN_USERNAME, ADMIN_PASSWORD);

        // 7. Admin UC-018: apply the exported definition -- it must install as a *new* survey.
        openAdmin("/survey-apply");
        String applyResult = new SurveyApplyPage(page).apply(elicitFile);
        assertTrue(applyResult.contains("New Survey Installed"), "unexpected apply outcome:\n" + applyResult);
        assertTrue(applyResult.contains(surveyKey), "apply dialog should echo the survey key:\n" + applyResult);

        // 8. Admin UC-003: register a respondent on the newly installed survey.
        String firstName = "E2E";
        String lastName = "Authored" + uniqueSuffix;
        String email = "e2e.authored." + uniqueSuffix + "@example.org";
        openAdmin("/register");
        new RegisterPage(page).registerSubject(surveyName, firstName, lastName, email);

        // 9. Admin UC-002: find them and capture the generated access code.
        openAdmin("/");
        SearchPage searchPage = new SearchPage(page);
        searchPage.searchByFirstAndLastName(firstName, lastName);
        assertEquals(1, searchPage.rowCount(), "expected exactly one matching subject after registration");
        String accessCode = searchPage.accessCodeAt(0);
        assertEquals("Not Started", searchPage.statusAt(0));

        // --- Survey -------------------------------------------------------------------------
        // 10. Survey UC-001: with two surveys installed the login page asks which one -- pick ours.
        openSurvey("/login/" + accessCode);
        new LoginPage(page).loginWithAccessCode(surveyName, accessCode);
        assertTrue(page.url().contains("/section"), "expected to land on the survey's first section");

        // 11. Survey UC-002 / UC-003 / UC-004: answer the single section, review, finish.
        new SectionPage(page).answerSurveyUntilReview();
        assertTrue(page.url().contains("/review"), "expected to reach the review page");
        new ReviewPage(page).finish();
        assertTrue(page.url().contains("/report"), "expected to be routed to the report page");

        // 12. Survey UC-005: the PDF still generates (empty report list) for a new tab.
        PdfDownload pdf = openPdfPopup(() -> new ReportPage(page).generatePdf());
        assertEquals("application/pdf", pdf.contentType(), "unexpected response from " + pdf.url());
        assertTrue(pdf.size() > 0, "expected a non-empty PDF from " + pdf.url());

        // 13. Survey UC-006: log out.
        openSurvey("/logout");

        // --- Admin, again -------------------------------------------------------------------
        // 14. Admin UC-002: the respondent now shows as Finished.
        openAdmin("/");
        searchPage.searchByAccessCode(accessCode);
        assertEquals(1, searchPage.rowCount());
        assertEquals("Finished", searchPage.statusAt(0));

        openAdmin("/logout");
    }
}
