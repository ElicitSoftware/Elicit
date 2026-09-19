package com.elicitsoftware.e2e.admin;

import com.elicitsoftware.e2e.PageObject;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.nio.file.Path;

/**
 * SurveyDefinitionApplyView (Admin UC-018, route {@code /survey-apply}): uploads a
 * {@code .elicit} survey definition exported by Author. The outcome is reported in a modal
 * dialog titled "New Survey Installed", "Survey Updated" or "Apply Failed" -- there is no
 * notification and no redirect -- so {@link #apply} returns the dialog's full text (title
 * included; read from the open {@code vaadin-dialog} host, whose light DOM holds the content)
 * for the caller to assert on, then closes it.
 */
public class SurveyApplyPage extends PageObject {

    public SurveyApplyPage(Page page) {
        super(page);
    }

    public String apply(Path elicitFile) {
        page.locator("#survey-apply-upload input[type=file]").setInputFiles(elicitFile);
        Locator dialog = page.locator("vaadin-dialog[opened]");
        // The host renders with display: contents (never "visible" to Playwright): wait for attachment.
        dialog.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));
        String text = dialog.innerText();
        dialog.locator("vaadin-button").filter(new Locator.FilterOptions().setHasText("Close")).click();
        return text;
    }
}
