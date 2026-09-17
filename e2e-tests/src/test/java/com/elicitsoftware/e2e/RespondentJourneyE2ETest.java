package com.elicitsoftware.e2e;

import com.elicitsoftware.e2e.admin.KeycloakLoginHelper;
import com.elicitsoftware.e2e.admin.RegisterPage;
import com.elicitsoftware.e2e.admin.SearchPage;
import com.elicitsoftware.e2e.survey.LoginPage;
import com.elicitsoftware.e2e.survey.ReportPage;
import com.elicitsoftware.e2e.survey.ReviewPage;
import com.elicitsoftware.e2e.survey.SectionPage;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The core respondent/report journey across both apps in a single browser session: register a
 * subject in Admin, search for them, take the survey in Survey, view/download the report, log
 * out, then come back to Admin to confirm status and generate the report there too.
 *
 * <p>Traceability: Admin UC-001 (authenticate), UC-003 (register a subject, which internally
 * exercises UC-015's token generation), UC-002 (search and monitor progress, verified twice),
 * UC-005 (generate and download a subject report). Survey UC-001 (enter via token), UC-002
 * (answer questions), UC-003 (review answers), UC-004 (finalize, triggered by Finish), UC-005
 * (view reports and download PDF), UC-006 (log out).</p>
 */
class RespondentJourneyE2ETest extends E2ETestBase {

    @Test
    void respondentJourneyEndToEnd() {
        // 1. Admin UC-001: authenticate through the real Keycloak OIDC redirect.
        openAdmin("/");
        new KeycloakLoginHelper(page).login(ADMIN_USERNAME, ADMIN_PASSWORD);

        // 2. Admin UC-003 (+ UC-015 implicitly): register a new subject.
        String uniqueSuffix = String.valueOf(System.nanoTime());
        String firstName = "E2E";
        String lastName = "Respondent" + uniqueSuffix;
        String email = "e2e." + uniqueSuffix + "@example.org";

        openAdmin("/register");
        new RegisterPage(page).registerSubject(firstName, lastName, email);

        // 3. Admin UC-002: find the new subject and capture its generated token.
        openAdmin("/");
        SearchPage searchPage = new SearchPage(page); // stateless wrapper around `page`; reused below
        searchPage.searchByFirstAndLastName(firstName, lastName);
        assertEquals(1, searchPage.rowCount(), "expected exactly one matching subject after registration");
        String token = searchPage.tokenAt(0);
        assertEquals("Not Started", searchPage.statusAt(0));

        // 4. Survey UC-001: enter the survey with the real token.
        openSurvey("/login/" + token);
        new LoginPage(page).loginWithToken(token);

        // 5. Survey UC-002: answer whatever the branching engine reveals, section by section.
        new SectionPage(page).answerSurveyUntilReview();
        assertTrue(page.url().contains("/review"), "expected to reach the review page");

        // 6. Survey UC-003 / UC-004: review, then finish (finalizes the respondent).
        new ReviewPage(page).finish();
        assertTrue(page.url().contains("/report"), "expected to be routed to the report page");

        // 7. Survey UC-005: generate the PDF -- opens in a new browser tab.
        Page pdfTab = waitForPopup(() -> new ReportPage(page).generatePdf());
        assertTrue(pdfTab.url().contains("/api/pdf/download"),
                "expected the new tab to show the PDF download endpoint");
        pdfTab.close();

        // 8. Survey UC-006: log out.
        openSurvey("/logout");

        // 9. Admin UC-002 (re-verified): the subject's status should now be "Finished".
        openAdmin("/");
        searchPage.searchByToken(token);
        assertEquals(1, searchPage.rowCount());
        assertEquals("Finished", searchPage.statusAt(0));

        // 10. Admin UC-005: generate the report from the search grid -- also a new tab.
        Page reportTab = waitForPopup(() -> searchPage.runRowAction(token, "Print Reports"));
        assertTrue(reportTab.url().contains("/api/pdf/download"),
                "expected the new tab to show the generated report PDF");
        reportTab.close();

        // 11. Admin UC-001 (logout path): leave the console.
        openAdmin("/logout");
    }
}
