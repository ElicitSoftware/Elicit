package com.elicitsoftware.e2e.multisite;

/**
 * One running Elicit site as the journey sees it: its name for messages and the base URLs of the
 * apps published on the host. {@code authorBaseUrl} is null on a site without Author (site 2).
 */
public record Site(String name, String surveyBaseUrl, String adminBaseUrl, String authorBaseUrl) {

    public static Site fromProperties(String prefix, String name, String surveyDefault, String adminDefault, String authorDefault) {
        return new Site(name,
                System.getProperty(prefix + ".survey.baseUrl", surveyDefault),
                System.getProperty(prefix + ".admin.baseUrl", adminDefault),
                authorDefault == null ? null : System.getProperty(prefix + ".author.baseUrl", authorDefault));
    }

    public boolean hasAuthor() {
        return authorBaseUrl != null;
    }

    @Override
    public String toString() {
        return name;
    }
}
