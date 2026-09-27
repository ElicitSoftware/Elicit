package com.elicitsoftware.e2e.author;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TranslationsView (Author UC-043 translate, UC-045 request a file, UC-046 import a returned one)
 * -- the per-survey page at {@code /survey/{id}/translations}.
 *
 * <p>Like every Author view this one sets no element ids, so the controls are located by their
 * visible labels ("Language", "Only what needs work", "Request translation", "Import a returned
 * file") and the grid is read with the flat, row-major {@code vaadin-grid-cell-content:visible}
 * technique the other grids use. The grid is built with {@code setAllRowsVisible(true)}, so there
 * is no virtual scroller to fight: every one of the survey's strings is in the DOM at once.</p>
 *
 * <p>The five columns are Where, Text, Original, Translation, Status. Rows are addressed by their
 * <em>Original</em> text, which is what a caller knows; the Translation cell is a
 * {@code vaadin-text-area} that saves on blur, so {@link #setTranslation} tabs out of it and waits
 * for the grid to be rebuilt with the new status.</p>
 */
public class TranslationsPage extends AuthorPageObject {

    /** Where, Text, Original, Translation, Status. */
    private static final int TOTAL_COLUMNS = 5;
    private static final int COL_WHERE = 0;
    private static final int COL_FIELD = 1;
    private static final int COL_ORIGINAL = 2;
    private static final int COL_TRANSLATION = 3;
    private static final int COL_STATUS = 4;

    /** "{0} translated, {1} missing, {2} out of date". */
    private static final Pattern COUNTS = Pattern.compile("(\\d+) translated, (\\d+) missing, (\\d+) out of date");

    /** The counts the page reports for the chosen language. */
    public record Counts(int translated, int missing, int stale) {
        public int total() {
            return translated + missing + stale;
        }
    }

    public TranslationsPage(Page page) {
        super(page);
    }

    /** Waits until the page has rendered its grid for a survey that publishes at least one language. */
    public void waitUntilLoaded() {
        page.locator("vaadin-grid").first().waitFor();
        countsSpan().waitFor();
    }

    /** The message shown instead of the grid when the survey publishes no language (UC-043 A4). */
    public boolean saysNoLanguages() {
        return page.getByText("This survey publishes no translations yet.").count() > 0;
    }

    /**
     * The language tags the page offers, i.e. the survey's published set (UC-044).
     *
     * <p>Read from the {@code vaadin-select-list-box}, which is where a {@code vaadin-select} keeps
     * its items -- in the host's own light DOM, in a {@code div[slot=overlay]} that the open
     * {@code vaadin-select-overlay} only portals visually (confirmed against the live app). Two
     * consequences: the items are readable without opening the select, and an unscoped
     * {@code vaadin-select-item} lookup also returns the copy of the selected item that the
     * {@code vaadin-select-value-button} renders, which is the chosen tag a second time.</p>
     */
    public List<String> languages() {
        Locator items = languageSelect().locator("vaadin-select-list-box vaadin-select-item");
        items.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));
        return items.allInnerTexts().stream().map(String::trim).toList();
    }

    /** Chooses the language whose tag is {@code tag} and waits for the grid to reload. */
    public void chooseLanguage(String tag) {
        Locator select = languageSelect();
        select.click(); // the items only become clickable once the overlay is open
        Locator item = select.locator("vaadin-select-list-box vaadin-select-item")
                .filter(new Locator.FilterOptions().setHasText(tag)).first();
        item.waitFor();
        item.click();
        page.waitForTimeout(800); // the grid and the counts are reloaded server-side
    }

    public Counts counts() {
        String text = countsSpan().innerText().trim();
        Matcher m = COUNTS.matcher(text);
        if (!m.find()) {
            throw new IllegalStateException("Unexpected counts line: " + text);
        }
        return new Counts(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
    }

    /**
     * UC-045: clicks "Request translation" and saves the hand-off document it downloads into
     * {@code targetDir}. The view renders a hidden download {@code Anchor} and clicks it from
     * JavaScript, so this is an ordinary browser download rather than a link the test can click.
     */
    public Path requestTranslationFile(Path targetDir) {
        Download download = page.waitForDownload(() -> buttonByText("Request translation").click());
        Path saved = targetDir.resolve(download.suggestedFilename());
        download.saveAs(saved);
        return saved;
    }

    /**
     * UC-046: uploads a returned hand-off document and returns the result dialog's full text
     * ("{n} imported, {n} unchanged, {n} left untranslated, {n} rejected." plus every rejection
     * and warning by name), then closes the dialog and waits for the grid to reload.
     */
    public String importTranslationFile(Path jsonFile) {
        Locator upload = page.locator("vaadin-upload").first();
        upload.waitFor();
        upload.locator("input[type=file]").setInputFiles(jsonFile);
        Locator dialog = page.locator("vaadin-dialog[opened]");
        dialog.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED).setTimeout(60_000));
        Locator close = dialog.locator("[id=\"translation-import-result-close\"]");
        close.waitFor();
        String text = dialog.innerText();
        close.click();
        dialog.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED));
        page.waitForTimeout(500);
        return text;
    }

    /** The "Only what needs work" filter (UC-043 step 4). */
    public void setOnlyOutstanding(boolean on) {
        Locator checkbox = page.locator("vaadin-checkbox:has(label:text-is(\"Only what needs work\"))").first();
        checkbox.waitFor();
        Locator input = checkbox.locator("input[type=checkbox]");
        if (input.isChecked() != on) {
            input.setChecked(on);
            page.waitForTimeout(800);
        }
    }

    /** How many rows the grid currently lists (respecting the filter). */
    public int rowCount() {
        return realRowBlocks().size();
    }

    /** The Original text of every listed row, in grid order. */
    public List<String> originals() {
        List<String> texts = cellTexts();
        List<String> out = new ArrayList<>();
        for (int block : realRowBlocks(texts)) {
            out.add(texts.get(block * TOTAL_COLUMNS + COL_ORIGINAL));
        }
        return out;
    }

    /** The translation currently shown for the row whose Original text is {@code original}. */
    public String translationOf(String original) {
        int block = rowBlockOf(original);
        return cells().nth(block * TOTAL_COLUMNS + COL_TRANSLATION).locator("textarea").inputValue();
    }

    /** "Translated", "Missing" or "Out of date" for the row whose Original text is {@code original}. */
    public String statusOf(String original) {
        return cellOf(original, COL_STATUS);
    }

    /** The "Where" cell of the row whose Original text is {@code original} ("Question Race", "List Race", ...). */
    public String whereOf(String original) {
        return cellOf(original, COL_WHERE);
    }

    /** The "Text" (field) cell of the row whose Original text is {@code original}. */
    public String fieldOf(String original) {
        return cellOf(original, COL_FIELD);
    }

    /**
     * UC-043 step 5: types a translation into the row whose Original text is {@code original} and
     * blurs the cell, which is what saves it. The whole grid is reloaded by the save, so this
     * waits for the row's status to become "Translated" rather than guessing at a settle time.
     */
    public void setTranslation(String original, String translation) {
        int block = rowBlockOf(original);
        Locator area = cells().nth(block * TOTAL_COLUMNS + COL_TRANSLATION).locator("textarea");
        area.fill(translation);
        area.press("Tab");
        page.waitForTimeout(1200);
        String status = statusOf(original);
        if (!"Translated".equals(status)) {
            throw new IllegalStateException("After translating '" + trim(original) + "' the row still reads "
                    + status + " (translation now '" + trim(translationOf(original)) + "')");
        }
    }

    // ---- locators -----------------------------------------------------------------------------

    private Locator languageSelect() {
        Locator select = page.locator("vaadin-select:has(label:text-is(\"Language\"))").first();
        select.waitFor();
        return select;
    }

    private Locator countsSpan() {
        // A span, not "any element with this text": every ancestor layout contains the same text,
        // and a page-wide text lookup can resolve to one of them. Only the counts span is a span
        // whose own text is the counts line -- the status badges are spans too, but read
        // "Translated" / "Missing" / "Out of date".
        return page.locator("span").filter(new Locator.FilterOptions().setHasText(COUNTS)).first();
    }

    private Locator cells() {
        return page.locator("vaadin-grid vaadin-grid-cell-content:visible");
    }

    /**
     * Every visible cell's text, in one round trip. The grid is built with
     * {@code setAllRowsVisible(true)}, so for this survey it is a hundred and thirty rows of five
     * columns in the DOM at once; reading them one {@code nth(...).innerText()} at a time cost
     * thousands of calls per assertion, which is slow enough to look like a hang.
     */
    private List<String> cellTexts() {
        Object texts = cells().evaluateAll("els => els.map(e => e.innerText.trim())");
        return ((List<?>) texts).stream().map(String::valueOf).toList();
    }

    /** Blocks (of {@link #TOTAL_COLUMNS} cells) that hold a real row; block 0 is the header. */
    private List<Integer> realRowBlocks() {
        return realRowBlocks(cellTexts());
    }

    private List<Integer> realRowBlocks(List<String> texts) {
        List<Integer> real = new ArrayList<>();
        for (int block = 1; block < texts.size() / TOTAL_COLUMNS; block++) {
            if (!texts.get(block * TOTAL_COLUMNS + COL_ORIGINAL).isEmpty()) {
                real.add(block);
            }
        }
        return real;
    }

    /** One cell of the row whose Original text is {@code original}, from a single snapshot. */
    private String cellOf(String original, int column) {
        List<String> texts = cellTexts();
        return texts.get(rowBlockOf(texts, original) * TOTAL_COLUMNS + column);
    }

    private int rowBlockOf(String original) {
        return rowBlockOf(cellTexts(), original);
    }

    private int rowBlockOf(List<String> texts, String original) {
        String wanted = original.trim();
        for (int block : realRowBlocks(texts)) {
            if (wanted.equals(texts.get(block * TOTAL_COLUMNS + COL_ORIGINAL))) {
                return block;
            }
        }
        throw new IllegalStateException("No translations row whose original text is '" + trim(original)
                + "' among the " + realRowBlocks(texts).size() + " rows listed");
    }

    private static String trim(String text) {
        return text == null ? "null" : text.length() <= 60 ? text : text.substring(0, 57) + "...";
    }
}
