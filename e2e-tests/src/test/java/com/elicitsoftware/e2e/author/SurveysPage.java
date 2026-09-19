package com.elicitsoftware.e2e.author;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SurveysView (Author UC-003 create, UC-006 open) -- the survey list at {@code /}.
 *
 * <p>The grid has no ids and no row-level markers, so rows are read with the same flat,
 * row-major {@code vaadin-grid-cell-content:visible} technique documented in Admin's
 * {@code SearchPage}: block 0 is the header, then one block of {@link #TOTAL_COLUMNS} per
 * rendered row. Surveys are listed in display order and a new survey gets the highest order,
 * so it is the last row -- which Vaadin's virtual scroller may not have rendered yet on a long
 * list; {@link #openSurvey} scrolls to the end before giving up.</p>
 */
public class SurveysPage extends AuthorPageObject {

    /** Name, Title, Order, Last edited by, Release note, actions. */
    private static final int TOTAL_COLUMNS = 6;
    private static final int COL_NAME = 0;
    private static final int COL_ACTIONS = 5;

    private static final Pattern SURVEY_URL = Pattern.compile("/survey/(\\d+)$");

    public SurveysPage(Page page) {
        super(page);
    }

    /** Waits until the list has rendered (the "New survey" button is the view's fixed anchor). */
    public void waitUntilLoaded() {
        buttonByText("New survey").waitFor();
    }

    /** UC-003: opens the "New survey" dialog, fills the required Name and Title, saves. */
    public void createSurvey(String name, String title) {
        buttonByText("New survey").click();
        Locator dialog = topDialog();
        fieldByLabel(dialog, "Name").fill(name);
        fieldByLabel(dialog, "Title").fill(title);
        submitDialog(dialog, "Save");
        page.locator("vaadin-notification-card")
                .filter(new Locator.FilterOptions().setHasText("Survey created"))
                .waitFor();
    }

    /** UC-006: clicks Open on the row named {@code name} and returns the survey's id from the URL. */
    public int openSurvey(String name) {
        for (int attempt = 0; attempt < 5; attempt++) {
            Integer block = findRowBlockByName(name);
            if (block != null) {
                buttonByText(cells().nth(block * TOTAL_COLUMNS + COL_ACTIONS), "Open").click();
                page.waitForURL(SURVEY_URL);
                Matcher m = SURVEY_URL.matcher(page.url());
                if (!m.find()) {
                    throw new IllegalStateException("Unexpected survey URL " + page.url());
                }
                return Integer.parseInt(m.group(1));
            }
            // Not rendered yet: scroll the virtual grid to its end and look again.
            page.locator("vaadin-grid").first().evaluate("g => g.scrollToIndex(Number.MAX_SAFE_INTEGER)");
            page.waitForTimeout(500);
        }
        throw new IllegalStateException("No survey row named " + name);
    }

    private Locator cells() {
        return page.locator("vaadin-grid vaadin-grid-cell-content:visible");
    }

    private Integer findRowBlockByName(String name) {
        Locator cells = cells();
        int totalBlocks = cells.count() / TOTAL_COLUMNS;
        for (int block = 1; block < totalBlocks; block++) {
            if (name.equals(cells.nth(block * TOTAL_COLUMNS + COL_NAME).innerText().trim())) {
                return block;
            }
        }
        return null;
    }
}
