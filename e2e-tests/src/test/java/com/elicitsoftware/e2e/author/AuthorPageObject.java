package com.elicitsoftware.e2e.author;

import com.elicitsoftware.e2e.PageObject;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * Shared helpers for the Author page objects. Unlike Admin and Survey, Author's views set no
 * element ids at all, so everything here is located by visible text: Vaadin fields carry their
 * label as a light-DOM {@code <label slot="label">} child of the field host, and buttons by their
 * caption. Dialogs are addressed through the topmost open {@code vaadin-dialog} host element:
 * that is where Vaadin keeps the dialog's content (and header title) in the light DOM -- the
 * {@code vaadin-dialog-overlay} beneath it has no light-DOM children of its own (confirmed
 * against the live app), so scoping to the overlay finds nothing.
 */
abstract class AuthorPageObject extends PageObject {

    protected AuthorPageObject(Page page) {
        super(page);
    }

    /** The native input/textarea of the field whose visible label is exactly {@code label}. */
    protected Locator fieldByLabel(Locator scope, String label) {
        String host = ":has(label:text-is(\"" + label + "\"))";
        return scope.locator("vaadin-text-field" + host + " input, "
                + "vaadin-text-area" + host + " textarea, "
                + "vaadin-combo-box" + host + " input");
    }

    protected Locator buttonByText(Locator scope, String text) {
        return scope.locator("vaadin-button:visible").filter(new Locator.FilterOptions().setHasText(text)).first();
    }

    protected Locator buttonByText(String text) {
        return buttonByText(page.locator("body"), text);
    }

    /**
     * The open dialog titled {@code title} (Vaadin labels a dialog with its header title), waited
     * for until attached. Dialogs are addressed by title rather than as "the last opened one":
     * a dialog opened from inside another (the question dialog's "New list…") is attached
     * inside its parent's light DOM a moment after the click, so a "last opened" lookup taken
     * right after the click resolves to the parent, and everything scoped to it -- fields,
     * Save -- silently targets the wrong dialog (confirmed live).
     */
    protected Locator dialog(String title) {
        Locator dialog = page.locator("vaadin-dialog[opened][aria-label=\"" + title + "\"]").last();
        dialog.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));
        return dialog;
    }

    /** The open dialog whose title starts with {@code prefix} (e.g. "Export " + survey title). */
    protected Locator dialogTitledLike(String prefix) {
        Locator dialog = page.locator("vaadin-dialog[opened][aria-label^=\"" + prefix + "\"]").last();
        dialog.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));
        return dialog;
    }

    /** Clicks {@code buttonText} in {@code dialog} (after the value-sync wait) and waits for it to close. */
    protected void submitDialog(Locator dialog, String buttonText) {
        clickAfterFill(buttonByText(dialog, buttonText));
        try {
            dialog.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED).setTimeout(10_000));
        } catch (com.microsoft.playwright.TimeoutError e) {
            // A dialog that stays open has refused the input: say what it shows instead of just timing out.
            String shown = dialog.count() > 0 ? dialog.innerText() : "(dialog gone)";
            String invalid = String.valueOf(page.locator("vaadin-dialog[opened] [invalid]").evaluateAll(
                    "els => els.map(e => e.tagName + '[' + (e.querySelector('label')?.textContent || '') + ']: '"
                            + " + (e.querySelector('[slot=error-message]')?.textContent || ''))"));
            String buttons = String.valueOf(dialog.locator("vaadin-button").evaluateAll(
                    "els => els.map(e => e.textContent.trim() + (e.offsetParent === null ? '(hidden)' : '') + (e.disabled ? '(disabled)' : ''))"));
            java.nio.file.Path shot = java.nio.file.Path.of("target", "dialog-" + System.currentTimeMillis() + ".png");
            page.screenshot(new Page.ScreenshotOptions().setPath(shot).setFullPage(true));
            throw new IllegalStateException("Dialog still open after '" + buttonText + "' (screenshot " + shot + "); invalid fields "
                    + invalid + "; buttons " + buttons + ":\n" + shown, e);
        }
    }
}
