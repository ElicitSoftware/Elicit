package com.elicitsoftware.e2e;

import com.elicitsoftware.e2e.admin.KeycloakLoginHelper;
import com.elicitsoftware.e2e.admin.RegisterPage;
import com.elicitsoftware.e2e.admin.SearchPage;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * "Importing" a respondent through the Admin UI via the RegisterView CSV upload control.
 *
 * <p>Traceability: Admin UC-003 alt-flow A6 (bulk upload by CSV). This is a different mechanism
 * from the bearer-token REST import in UC-011/UC-012, which stays out of scope for this browser
 * E2E suite (see the plan's scope notes) -- UC-003 A6 is the UI-driven "import a respondent"
 * path a real console user would take.</p>
 */
class RegisterViaCsvE2ETest extends E2ETestBase {

    @Test
    void csvUploadRegistersSubject() throws URISyntaxException {
        openAdmin("/");
        new KeycloakLoginHelper(page).login(ADMIN_USERNAME, ADMIN_PASSWORD);
        // Admin UC-028: a fresh database seeds no department, and the console blocks until one exists.
        bootstrapDepartment();

        openAdmin("/register");
        Path csvFile = fixture("fixtures/single-respondent.csv");
        new RegisterPage(page).uploadCsv(csvFile);

        openAdmin("/");
        SearchPage searchPage = new SearchPage(page);
        searchPage.searchByEmail("csv.e2e.test@example.org");
        assertEquals(1, searchPage.rowCount(), "expected the CSV-imported subject to be findable by email");
        assertEquals("Not Started", searchPage.statusAt(0));

        openAdmin("/logout");
    }

    private Path fixture(String classpathResource) throws URISyntaxException {
        URL url = getClass().getClassLoader().getResource(classpathResource);
        assertNotNull(url, "missing test fixture on classpath: " + classpathResource);
        return Path.of(url.toURI());
    }
}
