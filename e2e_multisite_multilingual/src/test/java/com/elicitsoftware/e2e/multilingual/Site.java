package com.elicitsoftware.e2e.multilingual;

/**
 * One running Elicit site as the journey sees it: its name for messages, the base URLs of the apps
 * published on the host, the language its respondents read, and the two chrome captions a
 * {@code SectionPage} needs in that language.
 *
 * <p>{@code authorBaseUrl} is null on a site without Author -- only the master (USA) has one.
 * {@code languageTag} is null on the master, which mounts no translations at all and therefore
 * serves English with no language selector; on a remote site it is the one tag mounted there, which
 * is also the tag the journey puts in the {@code ?lang=} of a respondent's link (Survey UC-009
 * step 2).</p>
 *
 * @param name          how the site is named in assertion messages
 * @param surveyBaseUrl the Survey application
 * @param adminBaseUrl  the Admin console
 * @param authorBaseUrl Author, or null where there is none
 * @param departmentName the department this site registers its subjects under
 * @param departmentCode its code, which is what a respondent export carries between sites
 * @param languageTag   the BCP-47 tag mounted here, or null for English only
 * @param nextCaption   {@code sectionView.btnNext} in that language
 * @param reviewCaption {@code sectionView.btnReview} in that language
 * @param rightToLeft   whether pages here are laid out right to left
 */
public record Site(String name, String surveyBaseUrl, String adminBaseUrl, String authorBaseUrl,
                   String departmentName, String departmentCode, String languageTag,
                   String nextCaption, String reviewCaption, boolean rightToLeft) {

    /**
     * The master: Author, Admin and Survey on the stock ports, English only.
     *
     * <p>Not "no language": USA mounts {@code i18n/usa}, which holds empty application directories.
     * The applications ship English, so the selector is hidden and every page reads English -- and
     * the survey's Spanish and Arabic content, though it travels in the same definition file, is
     * never served here, because a site serves a language only when its own chrome has it (Survey
     * UC-009 BR-009). The journey asserts exactly that.</p>
     */
    public static Site usa() {
        return new Site("USA",
                System.getProperty("usa.survey.baseUrl", "http://localhost:8080"),
                System.getProperty("usa.admin.baseUrl", "http://localhost:8081"),
                System.getProperty("usa.author.baseUrl", "http://localhost:8084"),
                "USA Clinic", "USA", null, "Next", "Review", false);
    }

    /** Mexico: Latin American Spanish, mounted from {@code i18n/mexico}. */
    public static Site mexico() {
        return new Site("Mexico",
                System.getProperty("mexico.survey.baseUrl", "http://localhost:8030"),
                System.getProperty("mexico.admin.baseUrl", "http://localhost:8031"),
                null, "Mexico Clinic", "MEX", "es-419", "Siguiente", "Revisar", false);
    }

    /** Arabia: Arabic, mounted from {@code i18n/arabia}, laid out right to left. */
    public static Site arabia() {
        return new Site("Arabia",
                System.getProperty("arabia.survey.baseUrl", "http://localhost:7980"),
                System.getProperty("arabia.admin.baseUrl", "http://localhost:7981"),
                null, "Arabia Clinic", "ARB", "ar", "التالي", "مراجعة", true);
    }

    public boolean hasAuthor() {
        return authorBaseUrl != null;
    }

    /** True where the survey's own content is expected in {@link #languageTag}, not in English. */
    public boolean servesTranslatedContent() {
        return languageTag != null;
    }

    /** {@code ?lang=<tag>} for this site's language, or "" on the English-only master. */
    public String langParameter() {
        return languageTag == null ? "" : "?lang=" + languageTag;
    }

    @Override
    public String toString() {
        return name;
    }
}
