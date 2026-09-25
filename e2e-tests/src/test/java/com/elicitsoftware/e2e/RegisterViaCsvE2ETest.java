package com.elicitsoftware.e2e;

import com.elicitsoftware.e2e.admin.KeycloakLoginHelper;
import com.elicitsoftware.e2e.admin.RegisterPage;
import com.elicitsoftware.e2e.admin.SearchPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * "Importing" a respondent through the Admin UI via the RegisterView CSV upload control.
 *
 * <p>Traceability: Admin UC-003 alt-flow A6 (bulk upload by CSV). This is a different mechanism
 * from the bearer-token REST import in UC-011/UC-012, which stays out of scope for this browser
 * E2E suite (see the plan's scope notes) -- UC-003 A6 is the UI-driven "import a respondent"
 * path a real console user would take.</p>
 */
class RegisterViaCsvE2ETest extends E2ETestBase {

    /** The placeholder {@code fixtures/single-respondent.csv} carries in place of an identity. */
    private static final String SUFFIX_PLACEHOLDER = "UNIQUE_SUFFIX";

    @TempDir
    Path tempDir;

    @Test
    void csvUploadRegistersSubject() throws URISyntaxException {
        openAdmin("/");
        new KeycloakLoginHelper(page).login(ADMIN_USERNAME, ADMIN_PASSWORD);
        // Admin UC-028: a fresh database seeds no department, and the console blocks until one exists.
        bootstrapDepartment();

        // The xid -- the subject's external identifier, an MRN in a clinical deployment -- has to
        // be new on every run. Admin matches an incoming subject by xid and department
        // (AccessCodeService -> Status.findByXidAndDepartmentId) and answers "Existing Subject"
        // without registering anyone when it finds one, so a fixture with a fixed (or, as this one
        // had, blank) xid stops testing registration the second time it is used against a database
        // -- and the V2 brownfield database still holds an earlier run's subject. The email is not
        // an identity here (households share one address); it varies only so the search below has
        // exactly one subject to find.
        String uniqueSuffix = String.valueOf(System.nanoTime());
        String email = "csv.e2e.test." + uniqueSuffix + "@example.org";

        openAdmin("/register");
        String importResult = new RegisterPage(page).uploadCsv(renderFixture(uniqueSuffix));
        assertTrue(importResult.contains("CSV import succeeded"),
                "unexpected CSV import outcome:\n" + importResult);
        assertTrue(importResult.contains("New Subject"),
                "the CSV import should have registered a new subject, not matched an existing one:\n"
                        + importResult);

        openAdmin("/");
        SearchPage searchPage = new SearchPage(page);
        searchPage.searchByEmail(email);
        assertEquals(1, searchPage.rowCount(), "expected the CSV-imported subject to be findable by email");
        assertEquals("Not Started", searchPage.statusAt(0));

        openAdmin("/logout");
    }

    /**
     * Writes the CSV fixture out with {@link #SUFFIX_PLACEHOLDER} replaced, so the upload
     * registers an identity no earlier run can have used. Returns the rendered file.
     */
    private Path renderFixture(String uniqueSuffix) throws URISyntaxException {
        Path template = fixture("fixtures/single-respondent.csv");
        try {
            String csv = Files.readString(template, StandardCharsets.UTF_8);
            assertTrue(csv.contains(SUFFIX_PLACEHOLDER),
                    template + " should carry " + SUFFIX_PLACEHOLDER + " in place of an identity");
            Path rendered = tempDir.resolve("single-respondent-" + uniqueSuffix + ".csv");
            Files.writeString(rendered, csv.replace(SUFFIX_PLACEHOLDER, uniqueSuffix), StandardCharsets.UTF_8);
            return rendered;
        } catch (IOException e) {
            throw new UncheckedIOException("could not render " + template, e);
        }
    }

    private Path fixture(String classpathResource) throws URISyntaxException {
        URL url = getClass().getClassLoader().getResource(classpathResource);
        assertNotNull(url, "missing test fixture on classpath: " + classpathResource);
        return Path.of(url.toURI());
    }
}
