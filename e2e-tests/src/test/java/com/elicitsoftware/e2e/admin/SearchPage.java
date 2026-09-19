package com.elicitsoftware.e2e.admin;

import com.elicitsoftware.e2e.PageObject;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;

/**
 * SearchView (Admin UC-002). Text filters and the Search button use the {@code .setId(...)}
 * calls added to {@code SearchView.java}. The department filter is left at its default "All
 * Departments" selection -- that already covers the seeded "Testing Department".
 *
 * <p>The grid itself has no per-row static ids (rendered by component-column renderers), so row
 * access reads Vaadin Grid's cell content elements ({@code <vaadin-grid-cell-content>}), which
 * render as light-DOM children of the grid slotted into its shadow-DOM table -- Playwright's
 * default CSS engine reaches them with a plain descendant selector, no shadow-piercing tricks
 * needed. Cell content is numbered flat and row-major across the currently-visible grid: block 0
 * is the header row, and each subsequent block of {@link #TOTAL_COLUMNS} is one *virtualized* DOM
 * row slot -- Vaadin's virtual scroller renders enough row slots to fill the viewport even when
 * there are fewer real results, leaving the extra slots' cell content empty.
 *
 * <p>Critically, the query filters to {@code :visible} elements: when a result set shrinks (e.g.
 * a search narrows to fewer rows), Vaadin's virtual scroller un-slots the now-unused
 * {@code <vaadin-grid-cell-content>} elements rather than removing them from the light DOM --
 * they remain queryable (and stale) but render with zero size. Without the {@code :visible}
 * filter, {@link #realRowBlocks()} would read leftover data from a *previous* search. This was
 * confirmed against the live grid (including cross-checking the actual executed SQL via Postgres
 * statement logging), not assumed.</p>
 */
public class SearchPage extends PageObject {

    /** 9 data columns + Edit + Action, matching SearchView.getSubjectGrid(...). */
    private static final int TOTAL_COLUMNS = 11;
    private static final int COL_ACCESS_CODE = 0;
    private static final int COL_STATUS = 8;
    private static final int COL_ACTION = 10;

    public SearchPage(Page page) {
        super(page);
    }

    public void searchByAccessCode(String accessCode) {
        input("access-code-filter").fill(accessCode);
        search();
    }

    public void searchByFirstAndLastName(String firstName, String lastName) {
        input("first-name-filter").fill(firstName);
        input("last-name-filter").fill(lastName);
        search();
    }

    public void searchByEmail(String email) {
        input("email-filter").fill(email);
        search();
    }

    public void searchByPhone(String phone) {
        input("phone-filter").fill(phone);
        search();
    }

    /**
     * Clears the access-code/name/email/phone text filters (leaves the department filter at its
     * default "All Departments"). UC-002 alt-flow A2 (clearing the department filter itself)
     * is not covered here.
     */
    public void clearTextFilters() {
        input("access-code-filter").fill("");
        input("first-name-filter").fill("");
        input("last-name-filter").fill("");
        input("email-filter").fill("");
        input("phone-filter").fill("");
        search();
    }

    /**
     * Clicks Search and gives the count+fetch round trip that repopulates the grid a moment to
     * complete before the caller reads results -- there's no element-appearance condition to
     * wait on (the grid DOM is already present either way), just a server round trip.
     */
    private void search() {
        clickAfterFill(byId("search-button"));
        page.waitForTimeout(500);
    }

    private Locator gridCellContents() {
        return page.locator("#subject-grid vaadin-grid-cell-content:visible");
    }

    /** Indices (into the flat cell-content list, as block numbers) of row blocks with real data. */
    private List<Integer> realRowBlocks() {
        Locator cells = gridCellContents();
        int totalBlocks = cells.count() / TOTAL_COLUMNS;
        List<Integer> real = new ArrayList<>();
        for (int block = 1; block < totalBlocks; block++) { // block 0 is the header row
            String token = cells.nth(block * TOTAL_COLUMNS + COL_ACCESS_CODE).innerText().trim();
            if (!token.isEmpty()) {
                real.add(block);
            }
        }
        return real;
    }

    public int rowCount() {
        return realRowBlocks().size();
    }

    public String accessCodeAt(int row) {
        int block = realRowBlocks().get(row);
        return gridCellContents().nth(block * TOTAL_COLUMNS + COL_ACCESS_CODE).innerText().trim();
    }

    public String statusAt(int row) {
        int block = realRowBlocks().get(row);
        return gridCellContents().nth(block * TOTAL_COLUMNS + COL_STATUS).innerText().trim();
    }

    private int findRowBlockByAccessCode(String accessCode) {
        for (int block : realRowBlocks()) {
            if (accessCode.equals(gridCellContents().nth(block * TOTAL_COLUMNS + COL_ACCESS_CODE).innerText().trim())) {
                return block;
            }
        }
        throw new IllegalStateException("No grid row found for access code " + accessCode);
    }

    /**
     * Selects the given action ("Send Email" / "Print Reports") on the row for {@code accessCode} and
     * clicks Submit. For "Print Reports" (Admin UC-005) this opens the generated PDF in a new
     * browser tab -- wrap the call in {@link com.elicitsoftware.e2e.E2ETestBase#waitForPopup}.
     */
    public void runRowAction(String accessCode, String action) {
        int block = findRowBlockByAccessCode(accessCode);
        Locator actionCell = gridCellContents().nth(block * TOTAL_COLUMNS + COL_ACTION);

        actionCell.locator("vaadin-combo-box input").click();
        Locator item = page.locator("vaadin-combo-box-item").filter(new Locator.FilterOptions().setHasText(action)).first();
        item.waitFor();
        item.click();

        actionCell.locator("vaadin-button").filter(new Locator.FilterOptions().setHasText("Submit")).click();
    }
}
