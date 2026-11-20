package com.elicitsoftware.e2e;

import com.elicitsoftware.e2e.admin.KeycloakLoginHelper;
import com.elicitsoftware.e2e.admin.RegisterPage;
import com.elicitsoftware.e2e.admin.SearchPage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Admin UC-002 (Search and Monitor Subject Progress): dedicated coverage of the individual text
 * filters beyond what {@link RespondentJourneyE2ETest} exercises in passing.
 */
class SearchFiltersE2ETest extends E2ETestBase {

    @Test
    void textFiltersNarrowToExpectedSubject() {
        openAdmin("/");
        new KeycloakLoginHelper(page).login(ADMIN_USERNAME, ADMIN_PASSWORD);
        // Admin UC-028: a fresh database seeds no department, and the console blocks until one exists.
        bootstrapDepartment();

        String uniqueSuffix = String.valueOf(System.nanoTime());
        String firstName = "Filter";
        String lastNameA = "Alpha" + uniqueSuffix;
        String lastNameB = "Beta" + uniqueSuffix;
        String emailA = "filter.alpha." + uniqueSuffix + "@example.org";
        String emailB = "filter.beta." + uniqueSuffix + "@example.org";

        openAdmin("/register");
        RegisterPage registerPage = new RegisterPage(page);
        registerPage.registerSubject(SEEDED_SURVEY_NAME, ADMIN_DEPARTMENT, firstName, lastNameA, emailA);
        openAdmin("/register");
        registerPage.registerSubject(SEEDED_SURVEY_NAME, ADMIN_DEPARTMENT, firstName, lastNameB, emailB);

        SearchPage searchPage = new SearchPage(page); // stateless wrapper around `page`; reused below

        openAdmin("/");
        // BR-008: text filters match case-insensitively as partial matches.
        searchPage.searchByFirstAndLastName(firstName, lastNameA.toLowerCase());
        assertEquals(1, searchPage.rowCount(), "expected the last-name filter to narrow to one subject");

        openAdmin("/");
        searchPage.searchByEmail(emailB);
        assertEquals(1, searchPage.rowCount(), "expected the email filter to narrow to one subject");

        openAdmin("/");
        searchPage.clearTextFilters();
        assertTrue(searchPage.rowCount() >= 2,
                "expected clearing the text filters (department stays \"All departments\") to show both subjects");

        openAdmin("/logout");
    }
}
