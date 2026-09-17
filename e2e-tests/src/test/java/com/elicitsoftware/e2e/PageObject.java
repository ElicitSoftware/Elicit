package com.elicitsoftware.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Base for the page-object helpers in this module.
 *
 * <p>Vaadin's web components ({@code vaadin-text-field}, {@code vaadin-checkbox}, etc.) render
 * their actual native {@code <input>}/{@code <textarea>} inside an open shadow root. Playwright's
 * CSS engine pierces shadow DOM by default, so {@code [id="host-id"] input} reaches the real
 * editable element with no special shadow-DOM handling -- see {@link #input(String)}.</p>
 *
 * <p>{@link #byId} and {@link #input} use the {@code [id="..."]} attribute-selector form rather
 * than {@code #id}: Survey's per-question ids are {@code DisplayKey} strings like
 * {@code 0001-0002-0000-0001-0000-0001-0000}, which start with a digit -- invalid as a plain CSS
 * {@code #foo} ident token, but perfectly fine as an attribute-selector value.</p>
 */
public abstract class PageObject {

    protected final Page page;

    protected PageObject(Page page) {
        this.page = page;
    }

    /** The native input/textarea nested inside the Vaadin field component with this id. */
    protected Locator input(String id) {
        return page.locator("[id=\"" + id + "\"] input, [id=\"" + id + "\"] textarea");
    }

    /** The Vaadin component (or plain element) itself, by id. */
    protected Locator byId(String id) {
        return page.locator("[id=\"" + id + "\"]");
    }

    /**
     * Clicks {@code button} after giving the last field filled just before it time to sync to
     * the server.
     *
     * <p>Playwright's {@code fill()} sets the DOM value and returns immediately -- it has no
     * concept of Vaadin's client-to-server value-changed round trip (unlike TestBench, which
     * waits for this automatically). Filling several fields in quick succession and then clicking
     * a button that reads their server-side state (e.g. a search/save button) can race ahead of
     * the last field's sync, silently acting on a stale (often empty) value for it -- confirmed
     * empirically against the live SearchView filters. This fixed, short wait is a deliberate,
     * pragmatic exception to "don't sleep, wait": there is no element-appearance condition to
     * wait on here, only a server round trip Playwright can't see.</p>
     */
    protected void clickAfterFill(Locator button) {
        page.waitForTimeout(300);
        button.click();
    }
}
