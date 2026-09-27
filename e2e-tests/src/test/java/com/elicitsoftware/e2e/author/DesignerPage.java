package com.elicitsoftware.e2e.author;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;

import java.util.List;

/**
 * SurveyDesignerView (Author UC-011/UC-014/UC-015/UC-016/UC-019) -- the flow-chart board at
 * {@code /survey/{id}/design}. Steps, sections and questions are added through the toolbar and
 * the per-node "⋮" menus rather than the drag-and-drop palette (every drag has a menu
 * equivalent, and a menu click is far more robust under automation).
 *
 * <p>Board nodes carry stable {@code data-node-id} attributes ({@code step-N}, {@code ss-N},
 * {@code sq-N}) and show their names in {@code h3.designer-lane-name} (steps),
 * {@code span.designer-card-name} (sections) and {@code span.designer-row-label} (questions), so
 * the multi-element helpers locate nodes by name. The board is rebuilt from scratch after every
 * change, so locators are always re-resolved and the post-condition waited on is the new node's
 * appearance.</p>
 */
public class DesignerPage extends AuthorPageObject {

    public DesignerPage(Page page) {
        super(page);
    }

    public void addStep(String name) {
        addStep(name, null);
    }

    /**
     * UC-016: adds a step; {@code dimensionName} (null = same as the name) is what the reporting
     * star schema records for it. Survey's {@code surveyreport.dim_step.value} is unique across
     * every survey of a site, so a test that installs the same survey repeatedly must give each
     * run its own dimension names or the Survey ETL refuses to start.
     */
    public void addStep(String name, String dimensionName) {
        buttonByText("Add step").click();
        saveElementDialog("New step", name, dimensionName);
        lane(name).waitFor();
    }

    /** Adds a section to the first step via the step's menu. */
    public void addSectionToFirstStep(String name) {
        openNodeMenu(firstStepLane(), "Add section");
        saveElementDialog("New section", name, null);
        firstSectionCard().waitFor();
    }

    /** UC-015: adds a section named {@code sectionName} to the step named {@code stepName}. */
    public void addSection(String stepName, String sectionName) {
        addSection(stepName, sectionName, null);
    }

    /** UC-015 with an explicit reporting dimension name (see {@link #addStep(String, String)}). */
    public void addSection(String stepName, String sectionName, String dimensionName) {
        openNodeMenu(lane(stepName), "Add section");
        saveElementDialog("New section", sectionName, dimensionName);
        card(stepName, sectionName).waitFor();
    }

    /**
     * Adds a question to the first section via the section's menu. The question type comes
     * from the drawer palette, whose default is TEXT -- the one type that needs nothing but its
     * text to be valid for export (choice types would require a select group).
     */
    public void addTextQuestionToFirstSection(String questionText) {
        openNodeMenu(firstSectionCard(), "Add question");
        Locator dialog = dialog("New question");
        fieldByLabel(dialog, "Question text").fill(questionText);
        submitDialog(dialog, "Save");
        page.locator("[data-node-id^=\"sq-\"]").first().waitFor();
    }

    /**
     * UC-011: adds a question of {@code type} (a question type name such as TEXT or INTEGER) to
     * the named section. {@code min}/{@code max} apply to INTEGER questions and may be null.
     */
    public void addQuestion(String stepName, String sectionName, String questionText, String type, Integer min, Integer max) {
        openNodeMenu(card(stepName, sectionName), "Add question");
        Locator dialog = dialog("New question");
        selectComboItem(dialog, "Type", type);
        fieldByLabel(dialog, "Question text").fill(questionText);
        if (min != null) {
            integerFieldByLabel(dialog, "Minimum").fill(String.valueOf(min));
        }
        if (max != null) {
            integerFieldByLabel(dialog, "Maximum").fill(String.valueOf(max));
        }
        submitDialog(dialog, "Save");
        row(questionText).waitFor();
    }

    /**
     * UC-011 + UC-014: adds a COMBOBOX question whose options come from a new list created
     * inline through the question dialog's "New list…" button.
     */
    public void addChoiceQuestion(String stepName, String sectionName, String questionText, String listName, List<String> options) {
        openNodeMenu(card(stepName, sectionName), "Add question");
        Locator dialog = dialog("New question");
        selectComboItem(dialog, "Type", "COMBOBOX");
        fieldByLabel(dialog, "Question text").fill(questionText);
        buttonByText(dialog, "New list").click(); // caption "New list…"
        Locator listDialog = dialog("New list of options");
        fieldByLabel(listDialog, "Name").fill(listName);
        fillOptions(listDialog, options);
        submitDialog(listDialog, "Save");
        page.locator("vaadin-notification-card")
                .filter(new Locator.FilterOptions().setHasText("Created list")).first().waitFor();
        // The question dialog now shows the new list as the selected "List of options"; give
        // that round trip (items + value pushed to the combo box) a moment before saving,
        // otherwise the Save click is lost in the re-render (confirmed live).
        Locator selected = fieldByLabel(dialog, "List of options");
        selected.waitFor();
        page.waitForTimeout(800);
        submitDialog(dialog, "Save");
        row(questionText).waitFor();
    }

    /**
     * UC-019: adds a rule read from the question named {@code questionText}. {@code token} may
     * be null; {@code targetKind} is "Step", "Section" or "Question" and {@code targetLabel} is
     * text that identifies the target in the dialog's "Step › Section › Question" paths.
     */
    public void addRule(String questionText, String operator, String value, String action, String token,
                        String targetKind, String targetLabel) {
        openNodeMenu(row(questionText), "Add rule");
        Locator dialog = dialog("New rule");
        selectComboItem(dialog, "Operator", operator);
        if (value != null) {
            fieldByLabel(dialog, "Value").fill(value);
        }
        selectComboItem(dialog, "Action", action);
        if (token != null) {
            Locator tokenInput = fieldByLabel(dialog, "Token");
            tokenInput.fill(token);
            tokenInput.press("Enter");
        }
        dialog.locator("vaadin-radio-group:has(label:text-is(\"Target\")) vaadin-radio-button")
                .filter(new Locator.FilterOptions().setHasText(targetKind)).first().click();
        selectComboItem(dialog, "Target element", targetLabel);
        submitDialog(dialog, "Save");
        page.locator("vaadin-notification-card")
                .filter(new Locator.FilterOptions().setHasText("Rule created")).first().waitFor();
    }

    /**
     * UC-011: rewords the question whose board row reads {@code rowLabel}, expanding its step and
     * section first.
     *
     * <p>Use this rather than {@link #editQuestion} on a survey whose questions have short texts:
     * the board labels a question row with its short text and falls back to the question text only
     * when there is none (Author {@code SurveyStructureQuery}). The row therefore keeps the same
     * label across a rewording, which is also what this waits for afterwards.</p>
     *
     * @param rowLabel the row's label on the board -- the question's short text
     * @param newText  the new question text
     */
    public void editQuestionTextOfRow(String stepName, String sectionName, String rowLabel, String newText) {
        expandLane(stepName);
        expandCard(stepName, sectionName);
        openNodeMenu(row(rowLabel), "Edit");
        Locator dialog = dialog("Edit question");
        fieldByLabel(dialog, "Question text").fill(newText);
        submitDialog(dialog, "Save");
        row(rowLabel).waitFor();
    }

    /** UC-011: changes the text of the question currently named {@code oldText}, expanding its step and section first. */
    public void editQuestion(String stepName, String sectionName, String oldText, String newText) {
        expandLane(stepName);
        expandCard(stepName, sectionName);
        openNodeMenu(row(oldText), "Edit");
        Locator dialog = dialog("Edit question");
        fieldByLabel(dialog, "Question text").fill(newText);
        submitDialog(dialog, "Save");
        row(newText).waitFor();
    }

    /**
     * UC-014: replaces the option texts of the list named {@code listName} (in order) through
     * the question dialog of {@code questionText}, whose "List of options" row the list belongs
     * to -- the drawer's Lists panel offers the same "Edit…" dialog, but the question dialog is
     * reachable from the board without opening the drawer.
     */
    public void editListOptions(String listName, List<String> newOptions) {
        // The Lists panel is a collapsed vaadin-details in the drawer toolbox.
        Locator lists = page.locator("vaadin-details.designer-toolbox-panel")
                .filter(new Locator.FilterOptions().setHasText("Lists")).first();
        lists.waitFor();
        if (lists.getAttribute("opened") == null) {
            lists.locator("[slot=summary], vaadin-details-summary").first().click();
            page.waitForTimeout(400);
        }
        Locator listRow = lists.locator(".designer-panel-row:has(span.designer-panel-label:text-is(\"" + listName + "\"))").first();
        listRow.waitFor();
        openNodeMenu(listRow, "Edit");
        Locator dialog = dialog("Edit list of options");
        Locator rows = dialog.locator(".designer-option-row");
        for (int i = 0; i < newOptions.size(); i++) {
            rows.nth(i).locator("vaadin-text-field").first().locator("input").fill(newOptions.get(i));
        }
        submitDialog(dialog, "Save");
    }

    // ---- rules on the board (UC-020, UC-033) ----------------------------------------------

    /**
     * UC-033 BR-005: the arrows the board draws red -- rules whose target is not after the question
     * they read from. The arrow overlay is a Lit element, so its edges live in an open shadow root
     * and Playwright reaches them like any Vaadin component's internals; an edge carries the rule's
     * id in {@code data-edge-id}, and an invalid one the class {@code invalid}.
     */
    public Locator invalidRuleArrows() {
        return page.locator("elicit-rule-arrows g.edge.invalid");
    }

    /**
     * Waits until the board draws exactly {@code expected} red arrows. The overlay lays itself out
     * on an animation frame (and again as fonts and transitions settle), so a count taken the
     * moment the board renders is a race; this asks the overlay's own shadow root instead.
     */
    public void awaitInvalidRuleCount(int expected) {
        try {
            page.waitForFunction("expected => {"
                    + " const overlay = document.querySelector('elicit-rule-arrows');"
                    + " const root = overlay && overlay.shadowRoot;"
                    + " return !!root && root.querySelectorAll('g.edge.invalid').length === expected; }", expected);
        } catch (TimeoutError e) {
            // The raw timeout names only the predicate; say what the board actually draws.
            String drawn = String.valueOf(invalidRuleArrows().locator("text.label").allTextContents());
            throw new IllegalStateException("The board draws " + invalidRuleArrows().count()
                    + " invalid rule arrow(s), expected " + expected + ": " + drawn, e);
        }
    }

    /**
     * What the board writes beside an arrow: what the rule does ("Show Rent details"), with
     * "— points backwards" appended while it is invalid. SVG text, so read as text content.
     */
    public String ruleArrowLabel(Locator arrow) {
        return String.valueOf(arrow.locator("text.label").first().textContent()).trim();
    }

    /**
     * UC-019 + UC-033: opens the rule an arrow stands for and points it at another element.
     *
     * <p>The gesture is a click on the arrow's own action glyph -- which is also the handle for
     * dragging that end onto a new target, so the overlay only treats a press that never moves as
     * a selection. Playwright's click is exactly that. Afterwards the board is rebuilt from the
     * server, so the caller re-resolves any arrow locator it still holds.</p>
     *
     * @param arrow       one of {@link #invalidRuleArrows()} or any {@code g.edge} of the overlay
     * @param targetKind  "Step", "Section" or "Question"
     * @param targetLabel text identifying the new target in the dialog's "Step › Section › Question" paths
     */
    public void retargetRule(Locator arrow, String targetKind, String targetLabel) {
        arrow.locator("g.glyph").first().click();
        Locator dialog = dialog("Edit rule");
        dialog.locator("vaadin-radio-group:has(label:text-is(\"Target\")) vaadin-radio-button")
                .filter(new Locator.FilterOptions().setHasText(targetKind)).first().click();
        selectComboItem(dialog, "Target element", targetLabel);
        submitDialog(dialog, "Save");
        page.locator("vaadin-notification-card")
                .filter(new Locator.FilterOptions().setHasText("Rule updated")).first().waitFor();
    }

    /** Overview: back to the survey editor, which validates the survey again on arrival. */
    public void openOverview() {
        buttonByText("Overview").click();
        page.waitForURL(url -> url.matches(".*/survey/\\d+"));
    }

    // ---- expanding collapsed nodes --------------------------------------------------------

    /**
     * Expands the step lane if it is collapsed: a fresh design page opens every lane and card
     * collapsed (only freshly created nodes are auto-opened), and a collapsed node renders none
     * of its children. A single click on the header toggles after a short double-click grace.
     */
    public void expandLane(String stepName) {
        Locator lane = lane(stepName);
        lane.waitFor();
        if (!hasClass(lane, "open")) {
            lane.locator(".designer-lane-header .designer-card-toggle").first().click();
            page.locator("[data-node-id^=\"step-\"].open:has(h3.designer-lane-name:text-is(\"" + stepName + "\"))").first().waitFor();
            page.waitForTimeout(300);
        }
    }

    /** Expands the section card if it is collapsed (see {@link #expandLane}); the lane must be open. */
    public void expandCard(String stepName, String sectionName) {
        Locator card = card(stepName, sectionName);
        card.waitFor();
        if (!hasClass(card, "open")) {
            card.locator(".designer-card-header .designer-card-toggle").first().click();
            lane(stepName).locator("[data-node-id^=\"ss-\"].open:has(span.designer-card-name:text-is(\"" + sectionName + "\"))").first().waitFor();
            page.waitForTimeout(300);
        }
    }

    private static boolean hasClass(Locator node, String className) {
        String classes = node.getAttribute("class");
        return classes != null && java.util.Arrays.asList(classes.split("\\s+")).contains(className);
    }

    // ---- locators -------------------------------------------------------------------------

    private Locator firstStepLane() {
        return page.locator("[data-node-id^=\"step-\"]").first();
    }

    private Locator firstSectionCard() {
        return page.locator("[data-node-id^=\"ss-\"]").first();
    }

    /** The step lane whose name is exactly {@code stepName}. */
    public Locator lane(String stepName) {
        return page.locator("[data-node-id^=\"step-\"]:has(h3.designer-lane-name:text-is(\"" + stepName + "\"))").first();
    }

    /** The section card named {@code sectionName} inside the step named {@code stepName}. */
    public Locator card(String stepName, String sectionName) {
        return lane(stepName).locator("[data-node-id^=\"ss-\"]:has(span.designer-card-name:text-is(\"" + sectionName + "\"))").first();
    }

    /** The question row whose label is exactly {@code questionText}. */
    public Locator row(String questionText) {
        return page.locator("[data-node-id^=\"sq-\"]:has(span.designer-row-label:text-is(\"" + questionText + "\"))").first();
    }

    private Locator integerFieldByLabel(Locator scope, String label) {
        return scope.locator("vaadin-integer-field:has(label:text-is(\"" + label + "\")) input");
    }

    /** Picks {@code itemText} in the combo box labeled {@code label} inside {@code scope}. */
    private void selectComboItem(Locator scope, String label, String itemText) {
        Locator input = scope.locator("vaadin-combo-box:has(label:text-is(\"" + label + "\")) input");
        input.click();
        Locator item = page.locator("vaadin-combo-box-item")
                .filter(new Locator.FilterOptions().setHasText(itemText)).first();
        item.waitFor();
        item.click();
    }

    /**
     * Adds one option row per text in the "New list of options" dialog. Every "Add option" click
     * re-renders all rows (SelectGroupDialog.renderRows), so each click gets a settle wait and the
     * display-text field is addressed by position: two text fields per row, display text first.
     */
    private void fillOptions(Locator listDialog, List<String> options) {
        for (int i = 0; i < options.size(); i++) {
            buttonByText(listDialog, "Add option").click();
            page.waitForTimeout(500);
            Locator rows = listDialog.locator(".designer-option-row");
            rows.nth(i).waitFor();
            rows.nth(i).locator("vaadin-text-field").first().locator("input").fill(options.get(i));
        }
    }

    /** Step and section share the same element dialog ("New step" / "New section"): Name is the only required field. */
    private void saveElementDialog(String title, String name, String dimensionName) {
        Locator dialog = dialog(title);
        fieldByLabel(dialog, "Name").fill(name);
        if (dimensionName != null) {
            fieldByLabel(dialog, "Dimension name").fill(dimensionName);
        }
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
