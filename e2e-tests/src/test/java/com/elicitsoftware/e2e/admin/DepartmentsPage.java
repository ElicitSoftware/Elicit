package com.elicitsoftware.e2e.admin;

import com.elicitsoftware.e2e.PageObject;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * EditDepartmentView (Admin departments management) at {@code /edit-department/{id}} -- the
 * route the Departments view's "New Department" button navigates to with id {@code 0}. The
 * form's fields carry no ids, so they are located by label, and the save button reads "Create
 * department". A successful save navigates back to {@code /departments}; a duplicate name or
 * code stays on the form with a notification.
 */
public class DepartmentsPage extends PageObject {

    public DepartmentsPage(Page page) {
        super(page);
    }

    /** Fills the new-department form (already open at {@code /edit-department/0}) and saves. */
    public void createDepartment(String name, String code, String fromEmail) {
        fillAndCommit(fieldByLabel("Department name"), name);
        fillAndCommit(fieldByLabel("Department code"), code);
        Locator defaultMessage = fieldByLabel("Default message ID");
        if (defaultMessage.inputValue().isBlank()) {
            fillAndCommit(defaultMessage, "1");
        }
        fillAndCommit(fieldByLabel("From email"), fromEmail);
        // The save button is captioned "Create department" for a new one ("Update department"
        // when editing) and stays disabled until the binder is valid.
        Locator save = page.locator("vaadin-button").filter(new Locator.FilterOptions().setHasText("Create department")).first();
        save.waitFor();
        clickAfterFill(save);
        page.waitForURL(url -> url.endsWith("/departments"));
    }

    /** The native input of the Vaadin field whose label is exactly {@code label}. */
    private Locator fieldByLabel(String label) {
        String host = ":has(label:text-is(\"" + label + "\"))";
        return page.locator("vaadin-text-field" + host + " input, vaadin-email-field" + host + " input, vaadin-text-area" + host + " textarea");
    }

    /**
     * Fills the input and tabs out of it: the form's Binder validates on the server-side value
     * change, which a Vaadin text field only sends on blur/Enter, and the save button stays
     * disabled until the binder is valid.
     */
    private void fillAndCommit(Locator input, String value) {
        input.fill(value);
        input.press("Tab");
        page.waitForTimeout(200);
    }
}
