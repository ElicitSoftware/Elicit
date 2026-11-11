package com.elicitsoftware.e2e.admin;

import com.microsoft.playwright.Page;

/**
 * Drives Keycloak's own hosted login page (Admin UC-001) -- that page lives outside both apps'
 * DOM, so this targets Keycloak's default theme element ids ({@code username}, {@code password},
 * {@code kc-login}) directly. Verify these ids against the real deployment if the Keycloak theme
 * differs.
 */
public class KeycloakLoginHelper {

    private final Page page;

    public KeycloakLoginHelper(Page page) {
        this.page = page;
    }

    /** Call after navigating to a protected Admin URL that redirected here. */
    public void login(String username, String password) {
        page.locator("#username").fill(username);
        page.locator("#password").fill(password);
        // Keycloak's login form is a plain server-rendered page (no Vaadin round trip), so no
        // sync race here -- but a click immediately after fill can still race the second fill's
        // input event on a slow CI runner, so keep this consistent with the other page objects.
        page.waitForTimeout(150);
        page.locator("#kc-login").click();
    }
}
