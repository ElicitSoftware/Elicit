package com.elicitsoftware.e2e.admin;

import com.elicitsoftware.e2e.PageObject;
import com.microsoft.playwright.Page;

import java.nio.file.Path;

/**
 * RegisterView (Admin UC-003 main flow + alt-flow A6 CSV upload). Department is left untouched
 * -- RegisterView pre-selects it automatically when the signed-in user has exactly one
 * department, which is the case for the seeded {@code user} account / "Testing Department".
 */
public class RegisterPage extends PageObject {

    public RegisterPage(Page page) {
        super(page);
    }

    /** UC-003 main flow: fills the single-subject form and clicks Save. */
    public void registerSubject(String firstName, String lastName, String email) {
        input("register-first-name").fill(firstName);
        input("register-last-name").fill(lastName);
        input("register-email").fill(email);
        clickAfterFill(byId("register-save-button"));
    }

    /**
     * UC-003 alt-flow A6: uploads a CSV fixture through the CSV upload control. Vaadin's Upload
     * component visually hides its native {@code <input type=file>} behind a styled button;
     * Playwright's {@code setInputFiles} works on it directly regardless of visibility.
     */
    public void uploadCsv(Path csvFile) {
        page.locator("#register-csv-upload input[type=file]").setInputFiles(csvFile);
    }
}
