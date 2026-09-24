package com.elicitsoftware.e2e.admin;

import com.elicitsoftware.e2e.PageObject;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.nio.file.Path;

/**
 * RegisterView (Admin UC-003 main flow + alt-flow A6 CSV upload). RegisterView pre-selects the
 * department automatically when the signed-in user has exactly one, so the four-argument
 * {@link #registerSubject(String, String, String, String)} relies on that; nothing seeds a
 * department any more (Admin UC-028), so the one in play is whichever the suite created, and an
 * administrator who has created several has no prefill. Prefer the five-argument form, which
 * names the department and works either way.
 *
 * <p>The survey selector ({@code register-survey}) is pre-selected the same way when exactly
 * one survey is installed; with several installed it is required, so
 * {@link #registerSubject(String, String, String, String)} always picks the named survey
 * explicitly and works in both situations.</p>
 */
public class RegisterPage extends PageObject {

    public RegisterPage(Page page) {
        super(page);
    }

    /**
     * UC-003 main flow: picks {@code surveyName}, fills the single-subject form, clicks Save and
     * waits for the "Subject saved" notification (the save is a server round trip with no
     * navigation, so without this wait a caller navigating away immediately could race it).
     */
    public void registerSubject(String surveyName, String firstName, String lastName, String email) {
        selectSurvey(surveyName);
        input("register-first-name").fill(firstName);
        input("register-last-name").fill(lastName);
        input("register-email").fill(email);
        clickAfterFill(byId("register-save-button"));
        page.locator("vaadin-notification-card")
                .filter(new Locator.FilterOptions().setHasText("Subject saved")).first().waitFor();
    }

    /**
     * UC-003 main flow with an explicit department: needed once the signed-in user belongs to
     * more than one department, because RegisterView then leaves the department combo box
     * ({@code register-department}) empty and required.
     */
    public void registerSubject(String surveyName, String departmentName, String firstName, String lastName, String email) {
        selectSurvey(surveyName);
        input("register-department").click();
        Locator dept = page.locator("vaadin-combo-box-item")
                .filter(new Locator.FilterOptions().setHasText(departmentName)).first();
        dept.waitFor();
        dept.click();
        input("register-first-name").fill(firstName);
        input("register-last-name").fill(lastName);
        input("register-email").fill(email);
        clickAfterFill(byId("register-save-button"));
        page.locator("vaadin-notification-card")
                .filter(new Locator.FilterOptions().setHasText("Subject saved")).first().waitFor();
    }

    private void selectSurvey(String surveyName) {
        input("register-survey").click();
        Locator item = page.locator("vaadin-combo-box-item")
                .filter(new Locator.FilterOptions().setHasText(surveyName)).first();
        item.waitFor();
        item.click();
    }

    /**
     * UC-003 alt-flow A6: uploads a CSV fixture through the CSV upload control. Vaadin's Upload
     * component visually hides its native {@code <input type=file>} behind a styled button;
     * Playwright's {@code setInputFiles} works on it directly regardless of visibility. The CSV
     * import always registers against survey id 1 (CsvImportService), so no survey is chosen.
     */
    public void uploadCsv(Path csvFile) {
        page.locator("#register-csv-upload input[type=file]").setInputFiles(csvFile);
    }
}
