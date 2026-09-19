package com.elicitsoftware.e2e.author;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * SurveyDesignerView (Author UC-011/UC-015/UC-016) -- the flow-chart board at
 * {@code /survey/{id}/design}. Steps, sections and questions are added through the toolbar and
 * the per-node "⋮" menus rather than the drag-and-drop palette (every drag has a menu
 * equivalent, and a menu click is far more robust under automation).
 *
 * <p>Board nodes carry stable {@code data-node-id} attributes ({@code step-N}, {@code ss-N},
 * {@code sq-N}). The board is rebuilt from scratch after every change, so locators are always
 * re-resolved and the post-condition waited on is the new node's appearance.</p>
 */
public class DesignerPage extends AuthorPageObject {

    public DesignerPage(Page page) {
        super(page);
    }

    public void addStep(String name) {
        buttonByText("Add step").click();
        saveElementDialog(name);
        firstStepLane().waitFor();
    }

    /** Adds a section to the first step via the step's menu. */
    public void addSectionToFirstStep(String name) {
        openNodeMenu(firstStepLane(), "Add section");
        saveElementDialog(name);
        firstSectionCard().waitFor();
    }

    /**
     * Adds a question to the first section via the section's menu. The question type comes
     * from the drawer palette, whose default is TEXT -- the one type that needs nothing but its
     * text to be valid for export (choice types would require a select group).
     */
    public void addTextQuestionToFirstSection(String questionText) {
        openNodeMenu(firstSectionCard(), "Add question");
        Locator dialog = topDialog();
        fieldByLabel(dialog, "Question text").fill(questionText);
        submitDialog(dialog, "Save");
        page.locator("[data-node-id^=\"sq-\"]").first().waitFor();
    }

    private Locator firstStepLane() {
        return page.locator("[data-node-id^=\"step-\"]").first();
    }

    private Locator firstSectionCard() {
        return page.locator("[data-node-id^=\"ss-\"]").first();
    }

    /** Step and section share the same "New step"/"New section" dialog: Name is the only required field. */
    private void saveElementDialog(String name) {
        Locator dialog = topDialog();
        fieldByLabel(dialog, "Name").fill(name);
        submitDialog(dialog, "Save");
    }

    /**
     * Opens the node's own "⋮" menu (the first menu bar inside the node -- a step lane also
     * contains its section cards' menus, but its own header comes first in the DOM) and clicks
     * the item with the given label.
     */
    private void openNodeMenu(Locator node, String itemLabel) {
        node.locator("vaadin-menu-bar.designer-node-menu vaadin-menu-bar-button").first().click();
        Locator item = page.locator("vaadin-menu-bar-item, vaadin-context-menu-item")
                .filter(new Locator.FilterOptions().setHasText(itemLabel)).first();
        item.waitFor();
        item.click();
    }
}
