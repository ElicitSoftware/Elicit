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
        return scope.locator("vaadin-button").filter(new Locator.FilterOptions().setHasText(text)).first();
    }

    protected Locator buttonByText(String text) {
        return buttonByText(page.locator("body"), text);
    }

    /**
     * The most recently opened dialog. The host renders with {@code display: contents}, so it
     * never counts as "visible" to Playwright -- wait for it to be attached instead.
     */
    protected Locator topDialog() {
        Locator dialog = page.locator("vaadin-dialog[opened]").last();
        dialog.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));
        return dialog;
    }

    /** Clicks {@code buttonText} in {@code dialog} (after the value-sync wait) and waits for it to close. */
    protected void submitDialog(Locator dialog, String buttonText) {
        clickAfterFill(buttonByText(dialog, buttonText));
        dialog.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED));
    }
}
