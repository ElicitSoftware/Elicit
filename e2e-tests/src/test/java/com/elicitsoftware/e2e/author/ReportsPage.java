package com.elicitsoftware.e2e.author;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Author's Reports page, {@code survey/{id}/reports} (UC-048 report services, UC-049 report
 * marks). Opened by URL; the editor's toolbar caption is "Reports…".
 */
public class ReportsPage extends AuthorPageObject {

    /** Columns between the name cell and the actions cell: description, address, actions. */
    private static final int SERVICE_COLUMNS_AFTER_NAME = 3;

    public ReportsPage(Page page) {
        super(page);
    }

    public void waitUntilLoaded() {
        page.locator("h3:has-text(\"Report services\")").first().waitFor();
    }

    /**
     * True while a row carries the "Built-in" badge (UC-048 BR-002). Only visible cells count:
     * the grid keeps the cell contents of removed rows in the light DOM, hidden.
     */
    public boolean listsTheBuiltInReport() {
        return servicesGrid().locator("vaadin-grid-cell-content:visible span.designer-badge")
                .filter(new Locator.FilterOptions().setHasText("Built-in")).count() > 0;
    }

    /**
     * Removes the built-in respondent report (UC-048 A3): Remove on its row, then the confirm
     * dialog, then the empty-list paragraph.
     * <p>
     * The badge and the buttons are in different cells of the row, so the row is found the way
     * the other page objects read a grid: the flat, row-major {@code vaadin-grid-cell-content}
     * list. The services grid has five columns (order, name, description, address, actions), so
     * the actions cell is three cells after the name cell that carries the badge.
     */
    public void removeBuiltInReport() {
        Locator badge = servicesGrid().locator("vaadin-grid-cell-content:visible")
                .filter(new Locator.FilterOptions().setHas(page.locator("span.designer-badge")))
                .filter(new Locator.FilterOptions().setHasText("Built-in")).first();
        badge.waitFor();
        Locator cells = servicesGrid().locator("vaadin-grid-cell-content:visible");
        int nameCell = -1;
        for (int i = 0; i < cells.count(); i++) {
            if (cells.nth(i).locator("span.designer-badge").count() > 0) {
                nameCell = i;
                break;
            }
        }
        if (nameCell < 0) {
            throw new IllegalStateException("UC-048 BR-002: no row carries the Built-in badge");
        }
        buttonByText(cells.nth(nameCell + SERVICE_COLUMNS_AFTER_NAME), "Remove").click();
        // The dialog host stays hidden and its overlay only displays it; the buttons are the host's
        // slotted children (slot="confirm-button"), which render through the overlay and are what
        // Playwright can click.
        page.locator("vaadin-confirm-dialog-overlay[opened]").last().waitFor();
        page.locator("vaadin-confirm-dialog[opened] vaadin-button[slot=confirm-button]").last().click();
        page.getByText("No report services.").first().waitFor();
    }

    /**
     * Mark element… (UC-049 steps 2-4): marks {@code elementLabel} for the respondent report, at
     * its default scope (wherever a question appears), under {@code label}.
     */
    public void markForRespondentReport(String elementLabel, String label) {
        buttonByText("Mark element").click(); // the caption is "Mark element…" (U+2026)
        Locator dialog = dialog("Mark for a report");
        Locator input = dialog.locator("vaadin-combo-box:has(label:text-is(\"Element\")) input");
        input.click();
        Locator item = page.locator("vaadin-combo-box-item")
                .filter(new Locator.FilterOptions().setHasText(elementLabel)).first();
        item.waitFor();
        item.click();
        fieldByLabel(dialog, "Label").fill(label);
        submitDialog(dialog, "Save");
        page.locator("vaadin-notification-card")
                .filter(new Locator.FilterOptions().setHasText("Marked")).first().waitFor();
    }

    /** The rows of the shown report's marks (the Respondent tab opens first). */
    public int markCount() {
        Locator grid = page.locator("h3:has-text(\"Report marks\") ~ vaadin-grid").first();
        if (grid.count() == 0 || !grid.isVisible()) {
            return 0;
        }
        return grid.locator("vaadin-grid-cell-content").filter(new Locator.FilterOptions().setHasText("Edit label")).count();
    }

    private Locator servicesGrid() {
        return page.locator("h3:has-text(\"Report services\") ~ vaadin-grid").first();
    }
}
