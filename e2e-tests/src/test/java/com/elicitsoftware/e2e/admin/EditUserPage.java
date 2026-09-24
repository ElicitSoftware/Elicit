package com.elicitsoftware.e2e.admin;

import com.elicitsoftware.e2e.PageObject;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * EditUserView at {@code /edit-user/{id}} (Admin user management): the only place a department
 * is assigned to a user, and RegisterView only offers the signed-in user's departments. The
 * seeded {@code admin} user is id 1 on a stock database. The "Departments" multi-select combo
 * box has no id, so it is located by label; picking an overlay item and pressing Escape leaves
 * the selection in place, as {@code SectionPage.selectFirstMultiSelectOption} documents.
 */
public class EditUserPage extends PageObject {

    public EditUserPage(Page page) {
        super(page);
    }

    /** Adds {@code departmentName} to the user's departments (no-op if already selected) and saves. */
    public void addDepartment(String departmentName) {
        Locator combo = page.locator("vaadin-multi-select-combo-box").filter(
                new Locator.FilterOptions().setHas(page.locator("label:has-text(\"Departments\")"))).first();
        combo.waitFor();
        boolean alreadySelected = (Boolean) combo.evaluate(
                "(el, name) => (el.selectedItems || []).some(d => d.name === name)", departmentName);
        if (!alreadySelected) {
            combo.locator("input").click();
            Locator item = page.locator("vaadin-multi-select-combo-box-item")
                    .filter(new Locator.FilterOptions().setHasText(departmentName)).first();
            item.waitFor();
            item.click(new Locator.ClickOptions().setForce(true));
            combo.locator("input").press("Escape");
        }
        clickAfterFill(page.locator("vaadin-button").filter(new Locator.FilterOptions().setHasText("Save")).first());
        page.locator("vaadin-notification-card")
                .filter(new Locator.FilterOptions().setHasText("User saved")).first().waitFor();
    }
}
