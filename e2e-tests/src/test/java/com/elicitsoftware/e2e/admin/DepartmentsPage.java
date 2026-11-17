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

    /**
     * Makes sure the department named {@code department} registers onto the survey named
     * {@code survey} (Admin UC-006 BR-172): opens it from the Departments list, adds the survey to
     * its Surveys selection when it is missing, and saves. A newly applied survey is in no
     * department (BR-173), so a test that registers onto one calls this after the apply.
     *
     * @return the survey's ID, read from the selection's label ("name (ID n)"), which a CSV row
     *         names its survey by (UC-003 BR-175)
     */
    public int ensureSurvey(String department, String survey) {
        openDepartment(department);
        Locator surveys = page.locator("#edit-department-surveys");
        surveys.waitFor();
        // Decide from the dropdown, not the chips: with several surveys selected the field folds
        // its chips into an overflow chip, and clicking an item that is already selected would
        // deselect it.
        Locator input = surveys.locator("input");
        input.click();
        input.fill(survey);
        Locator item = page.locator("vaadin-multi-select-combo-box-item")
                .filter(new Locator.FilterOptions().setHasText(survey + " (ID ")).first();
        item.waitFor();
        int id = idIn(item.innerText());
        boolean selected = item.getAttribute("selected") != null;
        if (!selected) {
            item.click();
        }
        input.press("Escape");
        page.waitForTimeout(300);
        if (!selected) {
            Locator save = page.locator("vaadin-button").filter(new Locator.FilterOptions().setHasText("Update department")).first();
            clickAfterFill(save);
            page.waitForURL(url -> url.endsWith("/departments"));
        }
        return id;
    }

    /** The n of a "name (ID n)" label. */
    private static int idIn(String label) {
        int start = label.lastIndexOf("(ID ") + 4;
        return Integer.parseInt(label.substring(start, label.indexOf(')', start)).trim());
    }

    /**
     * Opens a department's edit screen from the Departments list. Vaadin Grid slots each cell into
     * its row in the shadow DOM, so the row's Edit button is found through the name cell's slot.
     */
    private void openDepartment(String department) {
        page.navigate(page.url().replaceAll("^(https?://[^/]+).*$", "$1") + "/departments");
        page.locator("vaadin-grid-cell-content").filter(new Locator.FilterOptions().setHasText(department)).first().waitFor();
        String slot = (String) page.evaluate("name => {"
                + "const cell = [...document.querySelectorAll('vaadin-grid-cell-content')]"
                + "  .find(c => c.textContent.trim() === name);"
                + "if (!cell) return null;"
                + "const grid = cell.closest('vaadin-grid');"
                + "const tr = grid.shadowRoot.querySelector(`slot[name=\"${cell.getAttribute('slot')}\"]`).closest('tr');"
                + "for (const s of tr.querySelectorAll('slot')) {"
                + "  const c = document.querySelector(`vaadin-grid-cell-content[slot=\"${s.getAttribute('name')}\"]`);"
                + "  if (c && c.querySelector('vaadin-button')) return s.getAttribute('name');"
                + "}"
                + "return null; }", department);
        if (slot == null) {
            throw new IllegalStateException("no department named " + department + " on the Departments page");
        }
        page.locator("vaadin-grid-cell-content[slot=\"" + slot + "\"] vaadin-button").first().click();
        page.waitForURL(url -> url.contains("/edit-department/"));
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
