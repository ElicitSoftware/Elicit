package com.elicitsoftware.e2e.multilingual;

import com.elicitsoftware.e2e.admin.DepartmentsPage;
import com.elicitsoftware.e2e.admin.KeycloakLoginHelper;
import com.elicitsoftware.e2e.admin.RegisterPage;
import com.elicitsoftware.e2e.admin.RespondentImportPage;
import com.elicitsoftware.e2e.admin.SearchPage;
import com.elicitsoftware.e2e.admin.SurveyApplyPage;
import com.elicitsoftware.e2e.author.DesignerPage;
import com.elicitsoftware.e2e.author.ImportDefinitionPage;
import com.elicitsoftware.e2e.author.SurveyEditorPage;
import com.elicitsoftware.e2e.author.SurveysPage;
import com.elicitsoftware.e2e.author.TranslationsPage;
import com.elicitsoftware.e2e.survey.LoginPage;
import com.elicitsoftware.e2e.survey.ReviewPage;
import com.elicitsoftware.e2e.survey.SectionPage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The multilingual multi-site theory, end to end.
 *
 * <p>An English master (USA) imports {@code samples/census-household-survey.elicit} into Author,
 * declares Latin American Spanish and Arabic as the languages the survey is published in,
 * translates it through the hand-off file, and exports one definition. That one file is applied at
 * the master and at two remote sites whose translations mounts hold Spanish (Mexico) and Arabic
 * (Arabia); respondents at each site answer the survey in that site's language while the master's
 * own respondents read English, because the master mounts no translations and a site serves a
 * language only when its own chrome has it. The master then rewords a question, which puts both
 * translations out of date, retranslates them by hand, exports revision 2 and distributes it; the
 * respondents who were part-way through keep the wording they started under while new ones get the
 * new one, in their own language. Finally every remote respondent is exported and imported into the
 * master.</p>
 *
 * <p>One ordered story, one phase per test method. A failed phase marks the rest as skipped
 * ("the story already broke at N"), so the surefire report shows where it stopped. Every persona
 * visit runs in a fresh browser context, which matters here because the chosen language lives in
 * the browser session.</p>
 *
 * <p>Traceability: Author UC-005, UC-006, UC-007, UC-008, UC-011, UC-043, UC-044, UC-045, UC-046.
 * Admin UC-001, UC-002, UC-003, UC-011, UC-012, UC-017, UC-018, UC-028. Survey UC-001 to UC-006
 * and UC-009 (BR-005, BR-008, BR-009, BR-010).</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CensusMultilingualE2ETest extends MultilingualTestBase {

    /** The sixteen {@code @Order}ed phases below, as a recording's captions count them. */
    private static final int PHASES = 16;

    private static final Pattern SURVEY_KEY_LINE = Pattern.compile("(?m)^# survey_key: (\\S+)$");
    private static final Pattern TRANSLATIONS_COUNT = Pattern.compile("(?m)^# translations: (\\d+)$");

    /** The definition the master imports. */
    private static final Path DEFINITION = Path.of(System.getProperty("census.definition",
            "../samples/census-household-survey.elicit"));

    private static final String SPANISH = "es-419";
    private static final String ARABIC = "ar";
    /** Declared in the order UC-044 normalizes them to, so the page's own order can be asserted. */
    private static final List<String> PUBLISHED = List.of(ARABIC, SPANISH);

    /** The Race list, in display order -- the six options a respondent reads. */
    private static final List<String> RACE_OPTIONS = List.of(
            "White", "Black or African American", "American Indian or Alaska Native", "Asian",
            "Native Hawaiian or Other Pacific Islander", "Some other race or origin");
    /** The Languages list, in display order. */
    private static final List<String> LANGUAGE_OPTIONS = List.of(
            "English", "Spanish", "Arabic", "Chinese", "Another language");
    /**
     * The one string deliberately left untranslated in Arabic, to show a respondent reading it in
     * the survey's base language while everything around it is Arabic (Survey UC-009 A5, BR-005).
     */
    private static final String UNTRANSLATED_IN_ARABIC = "Chinese";

    private final ContentTranslations translations = ContentTranslations.load();

    /** Distinguishes this run's respondents; the survey itself is always the one census survey. */
    private final String runId = String.valueOf(System.nanoTime());
    private Path exportDir;
    private int surveyIdInAuthor;
    private String surveyKey;
    private Path v1File;
    private Path v2File;
    /** How many translatable strings the survey has, read from the Translations page. */
    private int stringCount;
    /** Access codes by respondent label (usa1..usa3, mex1..mex3, arb1..arb3). */
    private final Map<String, String> codes = new LinkedHashMap<>();
    /** Respondent export files by label. */
    private final Map<String, Path> remoteExports = new LinkedHashMap<>();
    private String failedPhase;

    // ---- phase plumbing ----------------------------------------------------------------------

    /**
     * Runs one phase of the story, unless an earlier one broke it.
     *
     * @param name      the journey's own short name for the phase, opening with its number -- what
     *                  a skipped phase names as the place the story broke
     * @param narration the sentence a viewer of a recording is shown while this phase runs
     *                  ({@link Recording}); it is nothing to the test itself
     */
    private void phase(String name, String narration, PhaseBody body) {
        assumeTrue(failedPhase == null, "skipped: the story already broke at " + failedPhase);
        Recording.expectPhases(PHASES);
        Recording.phase(numberOf(name), name, narration);
        try {
            body.run();
        } catch (Throwable t) {
            failedPhase = name;
            if (t instanceof RuntimeException re) {
                throw re;
            }
            if (t instanceof Error e) {
                throw e;
            }
            throw new RuntimeException(t);
        }
    }

    /** The phase number the name opens with, which is also its {@code @Order}. */
    private static int numberOf(String phaseName) {
        int end = 0;
        while (end < phaseName.length() && Character.isDigit(phaseName.charAt(end))) {
            end++;
        }
        return end == 0 ? 0 : Integer.parseInt(phaseName.substring(0, end));
    }

    @FunctionalInterface
    private interface PhaseBody {
        void run() throws Exception;
    }

    private Path exportDir() throws IOException {
        if (exportDir == null) {
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            exportDir = Files.createDirectories(Path.of("target", "exports", stamp));
        }
        return exportDir;
    }

    // ---- phases -----------------------------------------------------------------------------

    /**
     * Author UC-005 and UC-007: the master imports the definition drafted outside Author and checks
     * it. A survey that arrives with findings is not fit to publish, so the phase fails with the
     * validation panel's own text -- which is how a real problem in the sample surfaces here rather
     * than three phases later as a mystery.
     */
    @Test
    @Order(1)
    void phase01_masterImportsAndValidatesTheDefinition() {
        phase("1 import + validate",
                "The master imports the survey definition into Author, which validates it", () -> visit(page -> {
            assertTrue(Files.exists(DEFINITION), "the definition to import is missing: " + DEFINITION.toAbsolutePath());
            authorLogin(page, USA);
            SurveysPage surveys = new SurveysPage(page);
            surveys.waitUntilLoaded();
            assertTrue(!surveys.hasSurvey(CensusHouseholdSurvey.NAME),
                    "'" + CensusHouseholdSurvey.NAME + "' already exists in Author: the databases were not cleared"
                            + " since the last run. Run ./reset.sh all (or ./run.sh) first.");

            Recording.beat(USA, "importing " + DEFINITION.getFileName() + " into Author");
            openAuthor(page, USA, "/import");
            ImportDefinitionPage importer = new ImportDefinitionPage(page);
            String outcome = importer.importFile(DEFINITION);
            assertTrue(importer.succeeded(), "the import did not succeed:\n" + outcome);
            assertTrue(outcome.contains("Imported " + CensusHouseholdSurvey.NAME),
                    "unexpected import outcome:\n" + outcome);
            surveyIdInAuthor = importer.openInEditor();

            Recording.beat(USA, "Author validates it: ready to publish");
            SurveyEditorPage editor = new SurveyEditorPage(page);
            assertTrue(editor.isReadyToExport(),
                    "the imported definition does not validate (Author UC-007):\n" + editor.validationText());
        }));
    }

    /** Author UC-044: the master declares the two languages the survey is published in. */
    @Test
    @Order(2)
    void phase02_masterPublishesSpanishAndArabic() {
        phase("2 publish languages",
                "The author declares the survey published in Spanish and Arabic", () -> visit(page -> {
            authorLogin(page, USA);
            openAuthor(page, USA, "/survey/" + surveyIdInAuthor);
            Recording.beat(USA, "published in " + SPANISH + " and " + ARABIC + ", base language English");
            SurveyEditorPage editor = new SurveyEditorPage(page);
            editor.setContentLanguages(SPANISH + "," + ARABIC, "en");
            editor.openTranslations();

            Recording.beat(USA, "the Translations page lists every translatable string, none translated");
            TranslationsPage page043 = new TranslationsPage(page);
            page043.waitUntilLoaded();
            assertEquals(PUBLISHED, page043.languages(),
                    "the Translations page should offer exactly the published set");
            stringCount = page043.counts().total();
            assertTrue(stringCount > 100, "the census survey should have well over a hundred translatable strings,"
                    + " the page counts " + stringCount);
            assertEquals(new TranslationsPage.Counts(0, stringCount, 0), page043.counts(),
                    "nothing should be translated yet");
        }));
    }

    /**
     * Author UC-043, UC-045 and UC-046: one string typed straight into the grid, then the rest
     * through the hand-off file, for each language. Arabic deliberately leaves one option
     * untranslated, so the counts, the "only what needs work" filter, the export warning and the
     * respondent's fallback all have something real to show.
     */
    @Test
    @Order(3)
    void phase03_masterTranslatesTheSurvey() {
        phase("3 translate",
                "The survey is translated: one string by hand, the rest through the hand-off document", () -> visit(page -> {
            authorLogin(page, USA);
            openAuthor(page, USA, "/survey/" + surveyIdInAuthor + "/translations");
            TranslationsPage view = new TranslationsPage(page);
            view.waitUntilLoaded();

            // --- Spanish: one string by hand (UC-043 step 5), the rest by file -------------------
            view.chooseLanguage(SPANISH);
            String title = CensusHouseholdSurvey.NAME;
            String titleEs = translations.translationOf(SPANISH, "surveys", "title", title);
            assertEquals("Missing", view.statusOf(title), "the survey title should start out untranslated");
            Recording.beat(USA, "Spanish: the survey title typed straight into the grid");
            view.setTranslation(title, titleEs);
            assertEquals(new TranslationsPage.Counts(1, stringCount - 1, 0), view.counts(),
                    "after one hand-typed translation");

            Recording.beat(USA, "Author writes the hand-off document for the translator");
            Path esRequest = view.requestTranslationFile(exportDir());
            ContentTranslations.Filled esFilled = translations.fill(esRequest, SPANISH);
            assertTrue(esFilled.isComplete(), "the fixture does not cover the whole survey in " + SPANISH
                    + ": " + esFilled.report());
            assertEquals(stringCount, esFilled.items(), "the request should carry every string");
            Recording.beat(USA, "the filled document comes back: " + (stringCount - 1)
                    + " imported, the hand-typed one unchanged");
            String esResult = view.importTranslationFile(esFilled.file());
            // The hand-typed title comes back identical, so it is counted as unchanged, not imported.
            assertTrue(esResult.contains((stringCount - 1) + " imported, 1 unchanged, 0 left untranslated, 0 rejected."),
                    "unexpected Spanish import result:\n" + esResult);
            assertEquals(new TranslationsPage.Counts(stringCount, 0, 0), view.counts(), "Spanish after the import");

            // --- Arabic: the same file, one option left empty (UC-046 step 5) -------------------
            view.chooseLanguage(ARABIC);
            assertEquals(new TranslationsPage.Counts(0, stringCount, 0), view.counts(),
                    "Arabic should be untouched by the Spanish import");
            Recording.beat(USA, "Arabic: the same round trip, with one answer option left empty on purpose");
            Path arRequest = view.requestTranslationFile(exportDir());
            ContentTranslations.Filled arFilled = translations.fillExcept(arRequest, ARABIC, UNTRANSLATED_IN_ARABIC);
            assertTrue(arFilled.isComplete(), "the fixture does not cover the whole survey in " + ARABIC
                    + ": " + arFilled.report());
            String arResult = view.importTranslationFile(arFilled.file());
            assertTrue(arResult.contains((stringCount - 1) + " imported, 0 unchanged, 1 left untranslated, 0 rejected."),
                    "unexpected Arabic import result:\n" + arResult);
            assertEquals(new TranslationsPage.Counts(stringCount - 1, 1, 0), view.counts(), "Arabic after the import");

            // UC-043 step 4: the filter shows exactly the string that is still missing.
            Recording.beat(USA, "\"only what needs work\" shows the one string still missing: " + UNTRANSLATED_IN_ARABIC);
            view.setOnlyOutstanding(true);
            assertEquals(1, view.rowCount(), "only the untranslated option should be listed");
            assertEquals(UNTRANSLATED_IN_ARABIC, view.originals().get(0));
            assertEquals("Missing", view.statusOf(UNTRANSLATED_IN_ARABIC));
            view.setOnlyOutstanding(false);

            // And Spanish is unaffected by the Arabic import.
            view.chooseLanguage(SPANISH);
            assertEquals(new TranslationsPage.Counts(stringCount, 0, 0), view.counts(), "Spanish after the Arabic import");
            assertEquals(translations.questionText(SPANISH, CensusHouseholdSurvey.Q_RACE_V1),
                    view.translationOf(CensusHouseholdSurvey.Q_RACE_V1), "the race question in Spanish");
        }));
    }

    /**
     * Author UC-008: one file for every site (UC-044 BR-003), carrying the survey's published set
     * and every translation of both languages. The export warns about Arabic's one gap and exports
     * anyway, because an incomplete language is a warning, not an error (UC-044 A1).
     */
    @Test
    @Order(4)
    void phase04_masterExportsRevision1() {
        phase("4 export v1",
                "Revision 1 is exported — one file for every site, carrying both languages", () -> visit(page -> {
            authorLogin(page, USA);
            openAuthor(page, USA, "/survey/" + surveyIdInAuthor);
            SurveyEditorPage editor = new SurveyEditorPage(page);
            String findings = editor.validationText();
            assertTrue(findings.contains(ARABIC + " is published but 1 string(s) are not translated"),
                    "validation should warn about the one untranslated Arabic string:\n" + findings);

            Recording.beat(USA, "an incomplete language is a warning, not an error — exporting anyway");
            v1File = editor.export("Census Household Survey, Spanish and Arabic", exportDir());
            String definition = Files.readString(v1File, StandardCharsets.UTF_8);
            assertTrue(definition.startsWith("# ELICIT_SURVEY_EXPORT_V1"), "unexpected export header");
            Matcher key = SURVEY_KEY_LINE.matcher(definition);
            assertTrue(key.find(), "the export should carry the survey key");
            surveyKey = key.group(1);

            Matcher count = TRANSLATIONS_COUNT.matcher(definition);
            assertTrue(count.find(), "the export should carry a translations count:\n" + headerOf(definition));
            // Every Spanish string plus every Arabic one but the deliberately missing option.
            assertEquals(stringCount * 2 - 1, Integer.parseInt(count.group(1)),
                    "the definition should carry both languages' translations:\n" + headerOf(definition));
            assertTrue(definition.lines().anyMatch(l -> l.startsWith("surveys: ") && l.contains("|" + ARABIC + "," + SPANISH)),
                    "the survey record should carry base_language and content_languages (Survey V019)");
            assertTrue(definition.contains(translations.questionText(SPANISH, CensusHouseholdSurvey.Q_RACE_V1)),
                    "the definition should carry the Spanish race question");
            assertTrue(definition.contains(translations.questionText(ARABIC, CensusHouseholdSurvey.Q_RACE_V1)),
                    "the definition should carry the Arabic race question");
        }));
    }

    /** Admin UC-028 then UC-018: the master creates its department and installs revision 1. */
    @Test
    @Order(5)
    void phase05_masterAppliesRevision1() {
        phase("5 USA apply v1",
                "The master creates its department and installs revision 1", () -> applyDefinition(USA, v1File, "New Survey Installed"));
    }

    /**
     * The same file at both remote sites, and the proof that each one's translations mount took
     * effect: its Survey login page offers that language, the master's offers no choice at all
     * (UC-009 step 4, A3), and Arabia's pages are laid out right to left (UC-009 step 3).
     */
    @Test
    @Order(6)
    void phase06_remoteSitesApplyRevision1() {
        phase("6 remote apply v1",
                "Mexico and Arabia apply the same file, and each one offers its own language", () -> {
            for (Site site : REMOTE_SITES) {
                applyDefinition(site, v1File, "New Survey Installed");
            }
            visit(page -> {
                Recording.beat(USA, "the master mounts no translations: no language selector at all");
                openSurvey(page, USA, "/");
                new LoginPage(page).waitUntilLoaded();
                assertTrue(!isLanguageSwitcherVisible(page),
                        "the master mounts no translations, so it should offer no language selector");
                assertEquals("ltr", documentDirection(page), "the master's pages are left to right");
            });
            for (Site site : REMOTE_SITES) {
                visit(page -> {
                    Recording.beat(site, "its Survey offers " + site.languageTag() + " in the selector");
                    openSurvey(page, site, "/");
                    new LoginPage(page).waitUntilLoaded();
                    assertTrue(isLanguageSwitcherVisible(page),
                            site + " mounts " + site.languageTag() + ", so it should offer a language selector");
                });
                visit(page -> {
                    Recording.beat(site, "and lays the page out "
                            + (site.rightToLeft() ? "right to left" : "left to right"));
                    openSurvey(page, site, "/" + site.langParameter());
                    new LoginPage(page).waitUntilLoaded();
                    assertEquals(site.rightToLeft() ? "rtl" : "ltr", documentDirection(page),
                            site + ": the layout direction of " + site.languageTag());
                });
                // The console has the language too, though this suite drives it in English.
                visit(page -> {
                    adminLogin(page, site);
                    Recording.beat(site, "the console has the language too, though this journey drives it in English");
                    // The console's own shell, so the switcher is probed against a rendered page.
                    page.locator("vaadin-app-layout").first().waitFor();
                    assertTrue(isLanguageSwitcherVisible(page),
                            site + "'s console should offer " + site.languageTag() + " as well");
                });
            }
        });
    }

    /** Three subjects at the master: the first finishes in English, the second stops part-way. */
    @Test
    @Order(7)
    void phase07_masterRespondentsOnRevision1() {
        phase("7 USA respondents v1",
                "Three subjects at the master read the survey in English", () -> {
            register(USA, "usa1", "usa2", "usa3");
            takeWholeSurvey(USA, "usa1", CensusHouseholdSurvey.Q_RACE_V1);
            startAndPause(USA, "usa2", CensusHouseholdSurvey.Q_RACE_V1);
            assertStatus(USA, Map.of("usa1", "Finished", "usa2", "In Progress", "usa3", "Not Started"));
        });
    }

    /** The same three at Mexico, reading the survey in Spanish. */
    @Test
    @Order(8)
    void phase08_mexicoRespondentsOnRevision1() {
        phase("8 Mexico respondents v1",
                "The same three at Mexico read the same survey in Spanish", () -> {
            register(MEXICO, "mex1", "mex2", "mex3");
            takeWholeSurvey(MEXICO, "mex1", CensusHouseholdSurvey.Q_RACE_V1);
            startAndPause(MEXICO, "mex2", CensusHouseholdSurvey.Q_RACE_V1);
            assertStatus(MEXICO, Map.of("mex1", "Finished", "mex2", "In Progress", "mex3", "Not Started"));
        });
    }

    /**
     * The same three at Arabia, reading the survey in Arabic, right to left -- and reading the one
     * option that was never translated in English, with everything around it in Arabic (UC-009 A5).
     */
    @Test
    @Order(9)
    void phase09_arabiaRespondentsOnRevision1() {
        phase("9 Arabia respondents v1",
                "And at Arabia, in Arabic, right to left — with one option still in English", () -> {
            register(ARABIA, "arb1", "arb2", "arb3");
            takeWholeSurvey(ARABIA, "arb1", CensusHouseholdSurvey.Q_RACE_V1);
            startAndPause(ARABIA, "arb2", CensusHouseholdSurvey.Q_RACE_V1);
            assertStatus(ARABIA, Map.of("arb1", "Finished", "arb2", "In Progress", "arb3", "Not Started"));
        });
    }

    /**
     * Author UC-011 then UC-043 A1: rewording the race question puts both translations out of date,
     * which the designer says so at the time (FR-060) and the Translations page shows; the author
     * retranslates both by hand and exports revision 2.
     */
    @Test
    @Order(10)
    void phase10_masterRewordsTheRaceQuestionAndRetranslatesIt() {
        phase("10 reword + retranslate",
                "Rewording the race question puts both translations out of date at once", () -> {
            visit(page -> {
                authorLogin(page, USA);
                openAuthor(page, USA, "/survey/" + surveyIdInAuthor + "/design");
                Recording.beat(USA, "rewording the race question in the designer");
                DesignerPage designer = new DesignerPage(page);
                // By the board row's label, which is the question's short text ("Race"), not its
                // text: the designer labels a question by its short text where it has one.
                designer.editQuestionTextOfRow("About You", "Race and language",
                        CensusHouseholdSurvey.RACE_ROW, CensusHouseholdSurvey.Q_RACE_V2);
                Recording.beat(USA, "the designer says at once which languages that invalidated");
                Locator warning = page.locator("vaadin-notification-card")
                        .filter(new Locator.FilterOptions().setHasText("are now out of date"));
                warning.first().waitFor();
                String text = warning.first().innerText();
                assertTrue(text.contains(ARABIC) && text.contains(SPANISH),
                        "the wording change should name both languages it invalidated: " + text);
            });
            visit(page -> {
                authorLogin(page, USA);
                openAuthor(page, USA, "/survey/" + surveyIdInAuthor + "/translations");
                TranslationsPage view = new TranslationsPage(page);
                view.waitUntilLoaded();

                Recording.beat(USA, "one row out of date per language; both retranslated by hand");
                view.chooseLanguage(SPANISH);
                assertEquals(new TranslationsPage.Counts(stringCount - 1, 0, 1), view.counts(),
                        "Spanish after the reword");
                assertEquals("Out of date", view.statusOf(CensusHouseholdSurvey.Q_RACE_V2));
                view.setTranslation(CensusHouseholdSurvey.Q_RACE_V2,
                        translations.questionText(SPANISH, CensusHouseholdSurvey.Q_RACE_V2));
                assertEquals(new TranslationsPage.Counts(stringCount, 0, 0), view.counts(),
                        "Spanish after retranslating the reworded question");

                view.chooseLanguage(ARABIC);
                assertEquals(new TranslationsPage.Counts(stringCount - 2, 1, 1), view.counts(),
                        "Arabic after the reword (one string was never translated)");
                view.setTranslation(CensusHouseholdSurvey.Q_RACE_V2,
                        translations.questionText(ARABIC, CensusHouseholdSurvey.Q_RACE_V2));
                assertEquals(new TranslationsPage.Counts(stringCount - 1, 1, 0), view.counts(),
                        "Arabic after retranslating the reworded question");
            });
            visit(page -> {
                authorLogin(page, USA);
                openAuthor(page, USA, "/survey/" + surveyIdInAuthor);
                Recording.beat(USA, "exporting revision 2");
                v2File = new SurveyEditorPage(page).export("Race question reworded", exportDir());
                String definition = Files.readString(v2File, StandardCharsets.UTF_8);
                assertTrue(definition.contains(CensusHouseholdSurvey.Q_RACE_V2),
                        "revision 2 should carry the new wording");
                assertTrue(definition.contains(translations.questionText(SPANISH, CensusHouseholdSurvey.Q_RACE_V2)),
                        "revision 2 should carry the new Spanish translation");
                assertTrue(definition.contains(translations.questionText(ARABIC, CensusHouseholdSurvey.Q_RACE_V2)),
                        "revision 2 should carry the new Arabic translation");
            });
        });
    }

    /** Admin UC-017: the master updates its own installation first. */
    @Test
    @Order(11)
    void phase11_masterAppliesRevision2() {
        phase("11 USA apply v2",
                "The master installs revision 2", () -> applyDefinition(USA, v2File, "Survey Updated"));
    }

    /**
     * Survey UC-009 BR-010 and the Type 2 snapshot: the respondent who was part-way through keeps
     * the wording they started under, and the one who had never logged in gets the new one.
     */
    @Test
    @Order(12)
    void phase12_masterRespondentsAfterRevision2() {
        phase("12 USA respondents v2",
                "Part-way through keeps the old wording; a fresh respondent gets the new", () -> {
            resumeAndFinish(USA, "usa2", CensusHouseholdSurvey.Q_RACE_V1);
            takeWholeSurvey(USA, "usa3", CensusHouseholdSurvey.Q_RACE_V2);
            assertStatus(USA, Map.of("usa2", "Finished", "usa3", "Finished"));
        });
    }

    /** The master shares revision 2; both remote sites apply the same file. */
    @Test
    @Order(13)
    void phase13_remoteSitesApplyRevision2() {
        phase("13 remote apply v2",
                "The master shares revision 2; both remote sites apply it", () -> {
            for (Site site : REMOTE_SITES) {
                applyDefinition(site, v2File, "Survey Updated");
            }
        });
    }

    /** The same snapshot rule at both remote sites, in their own language. */
    @Test
    @Order(14)
    void phase14_remoteRespondentsAfterRevision2() {
        phase("14 remote respondents v2",
                "The same rule holds at both remote sites, each in its own language", () -> {
            resumeAndFinish(MEXICO, "mex2", CensusHouseholdSurvey.Q_RACE_V1);
            takeWholeSurvey(MEXICO, "mex3", CensusHouseholdSurvey.Q_RACE_V2);
            assertStatus(MEXICO, Map.of("mex2", "Finished", "mex3", "Finished"));

            resumeAndFinish(ARABIA, "arb2", CensusHouseholdSurvey.Q_RACE_V1);
            takeWholeSurvey(ARABIA, "arb3", CensusHouseholdSurvey.Q_RACE_V2);
            assertStatus(ARABIA, Map.of("arb2", "Finished", "arb3", "Finished"));
        });
    }

    /** Admin UC-011: each remote site exports its three respondents, one file each. */
    @Test
    @Order(15)
    void phase15_remoteSitesExportTheirRespondents() {
        phase("15 remote export",
                "Each remote site exports its three respondents, one file each", () -> {
            exportRespondents(MEXICO, "mex1", "mex2", "mex3");
            exportRespondents(ARABIA, "arb1", "arb2", "arb3");
        });
    }

    /**
     * Admin UC-012: the master takes in all six. It needs a local department carrying each remote
     * site's code for the import to resolve, and creating one assigns it to its creator, which is
     * also what makes the imported respondents visible in Search (UC-028 BR-113).
     */
    @Test
    @Order(16)
    void phase16_masterImportsEveryRemoteRespondent() {
        phase("16 USA import",
                "The master takes in all six, and re-exports one byte for byte", () -> {
            visit(page -> {
                adminLogin(page, USA);
                assertNoBlockingDialog(page, USA);
                for (Site site : REMOTE_SITES) {
                    ensureDepartment(page, USA, site.departmentName(), site.departmentCode(),
                            site.name().toLowerCase() + "@example.org");
                }
                RespondentImportPage importer = new RespondentImportPage(page);
                for (Map.Entry<String, Path> e : remoteExports.entrySet()) {
                    Recording.beat(USA, "importing " + e.getKey() + "'s answers from " + siteOf(e.getKey()));
                    // A fresh view per file: the upload component accepts a single file per instance.
                    openAdmin(page, USA, "/respondent-import");
                    String result = importer.importFile(e.getValue());
                    assertTrue(result.contains("Import Successful"), e.getKey() + ": unexpected import outcome:\n" + result);
                }
            });
            visit(page -> {
                adminLogin(page, USA);
                openAdmin(page, USA, "/");
                SearchPage search = new SearchPage(page);
                List<String> problems = new ArrayList<>();
                for (String label : codes.keySet()) {
                    search.searchByAccessCode(codes.get(label));
                    if (search.rowCount() != 1) {
                        problems.add(label + ": expected 1 row, found " + search.rowCount());
                        continue;
                    }
                    if (!"Finished".equals(search.statusAt(0))) {
                        problems.add(label + ": status " + search.statusAt(0) + ", expected Finished");
                    }
                    String expectedDept = siteOf(label).departmentName();
                    if (!expectedDept.equals(search.departmentAt(0))) {
                        problems.add(label + ": department " + search.departmentAt(0) + ", expected " + expectedDept);
                    }
                }
                assertTrue(problems.isEmpty(), "the master after the import:\n" + String.join("\n", problems));

                Recording.beat(USA, "all nine respondents at the master, the imported six in the right department");
                // Round trip: re-exporting an imported respondent must reproduce the remote site's
                // answers and dependent lines (the V2 format carries no site-local ids in them).
                for (String label : List.of("mex1", "arb1")) {
                    search.searchByAccessCode(codes.get(label));
                    Path again = search.exportRespondent(codes.get(label),
                            Files.createDirectories(exportDir().resolve("usa-reexport")));
                    List<String> original = dataLines(Files.readString(remoteExports.get(label), StandardCharsets.UTF_8));
                    List<String> reexported = dataLines(Files.readString(again, StandardCharsets.UTF_8));
                    assertEquals(original, reexported, label + ": answers/dependents differ after the import round trip");
                }
            });
        });
    }

    // ---- personas ----------------------------------------------------------------------------

    private static void adminLogin(Page page, Site site) {
        Recording.beat(site, "an administrator signs in to the console");
        page.navigate(site.adminBaseUrl() + "/");
        new KeycloakLoginHelper(page).login(ADMIN_USERNAME, ADMIN_PASSWORD);
        page.waitForURL(url -> url.startsWith(site.adminBaseUrl()));
    }

    private static void authorLogin(Page page, Site site) {
        Recording.beat(site, "the author signs in");
        openAuthor(page, site, "/");
        new KeycloakLoginHelper(page).login(AUTHOR_USERNAME, AUTHOR_PASSWORD);
        page.waitForURL(url -> url.startsWith(site.authorBaseUrl()));
    }

    /**
     * Fails with a clear message rather than a mystery timeout when the console is blocked for want
     * of a department (Admin UC-028).
     */
    private static void assertNoBlockingDialog(Page page, Site site) {
        assertTrue(!isBlockingDepartmentDialogOpen(page),
                site + ": the console is blocked for want of a department; an earlier phase should have created one");
    }

    /**
     * Whether the on-page language selector is offered. It is in the DOM either way -- an
     * English-only deployment renders it hidden rather than leaving it out -- so this waits for it
     * to become <em>visible</em> and reads a timeout as "not offered". The wait also covers the
     * gap between a view's route being reached and Vaadin having rendered it: a bare
     * {@code isVisible()} taken too early answers "no" against a blank page.
     */
    private static boolean isLanguageSwitcherVisible(Page page) {
        try {
            page.locator("#language-switcher").first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(10_000));
            return true;
        } catch (com.microsoft.playwright.TimeoutError e) {
            return false;
        }
    }

    /**
     * Applies a definition at {@code site}, making sure the site has its department first.
     *
     * <p>Every site starts with none (Admin UC-028): nothing is seeded, so the first admin visit at
     * <em>each</em> site is blocked by a modal dialog until one exists, and creating it assigns it
     * to its creator. On a site that already has it this is a cheap no-op, which is why it is done
     * on every apply rather than only on the first.</p>
     */
    private void applyDefinition(Site site, Path file, String expectedOutcome) {
        visit(page -> {
            adminLogin(page, site);
            ensureDepartment(page, site, site.departmentName(), site.departmentCode(),
                    site.name().toLowerCase() + "@example.org");
            Recording.beat(site, "Apply Survey Definition: " + file.getFileName());
            openAdmin(page, site, "/survey-apply");
            String result = new SurveyApplyPage(page).apply(file);
            assertTrue(result.contains(expectedOutcome),
                    site + ": unexpected apply outcome, wanted '" + expectedOutcome + "':\n" + result);
            // Admin UC-018 BR-107: a successful apply asks Survey to rebuild the reporting star schema.
            assertTrue(result.contains("Reporting schema rebuilt."),
                    site + ": the apply should have rebuilt the reporting schema:\n" + result);
        });
    }

    /**
     * Gives the site's administrator the named department, creating it if it is missing
     * (Admin UC-028). On a freshly reset site the console blocks every screen with a modal dialog
     * until one exists; this follows the dialog's own remedy. On a site that already has it, the
     * dialog is absent and this is a cheap no-op.
     */
    private void ensureDepartment(Page page, Site site, String name, String code, String email) {
        if (isBlockingDepartmentDialogOpen(page)) {
            page.locator("#missing-department-add").click();
            page.waitForURL(url -> url.endsWith("/departments"));
        } else {
            openAdmin(page, site, "/departments");
            // The grid renders after the view's fixed "New Department" button; give it a moment
            // before deciding the department is missing.
            page.locator("vaadin-button").filter(new Locator.FilterOptions().setHasText("New Department")).first().waitFor();
            page.waitForTimeout(1000);
        }
        if (page.getByText(name, new Page.GetByTextOptions().setExact(true)).count() > 0) {
            return;
        }
        Recording.beat(site, "creating the " + name + " department (" + code + ")");
        openAdmin(page, site, "/edit-department/0");
        new DepartmentsPage(page).createDepartment(name, code, email);
    }

    private void register(Site site, String... labels) {
        visit(page -> {
            adminLogin(page, site);
            assertNoBlockingDialog(page, site);
            for (String label : labels) {
                Recording.beat(site, "registering subject " + label + " and taking their access code");
                openAdmin(page, site, "/register");
                String first = "Census";
                String last = label.toUpperCase() + runId;
                new RegisterPage(page).registerSubject(CensusHouseholdSurvey.NAME, site.departmentName(),
                        first, last, label + "." + runId + "@example.org");
                openAdmin(page, site, "/");
                SearchPage search = new SearchPage(page);
                search.searchByFirstAndLastName(first, last);
                assertEquals(1, search.rowCount(), "expected exactly one row for " + label + " on " + site);
                assertEquals("Not Started", search.statusAt(0));
                codes.put(label, search.accessCodeAt(0));
            }
        });
    }

    /** Logs a respondent in, in their site's language, and returns a section page that speaks it. */
    private SectionPage respondentLogin(Page page, Site site, String label) {
        Recording.beat(site, label + " opens their link and signs in with their access code"
                + (site.servesTranslatedContent() ? " (?lang=" + site.languageTag() + ")" : ""));
        openSurvey(page, site, "/login/" + codes.get(label) + site.langParameter());
        LoginPage login = new LoginPage(page);
        login.loginWithAccessCode(CensusHouseholdSurvey.NAME, codes.get(label));
        assertTrue(page.url().contains("/section"), label + " should land on a section, was " + page.url());
        if (site.rightToLeft()) {
            assertEquals("rtl", documentDirection(page), label + ": " + site + "'s pages are right to left");
        }
        return new SectionPage(page, site.nextCaption(), site.reviewCaption());
    }

    /** What this site's respondents should read for an English source string. */
    private String asRead(Site site, String englishQuestionText) {
        if (!site.servesTranslatedContent()) {
            return englishQuestionText;
        }
        String translated = translations.questionText(site.languageTag(), englishQuestionText);
        assertTrue(translated != null, "the fixture has no " + site.languageTag() + " for: " + englishQuestionText);
        return translated;
    }

    /** The race options as this site's respondents should read them. */
    private List<String> raceOptionsAt(Site site) {
        return optionsAt(site, RACE_OPTIONS);
    }

    private List<String> optionsAt(Site site, List<String> englishOptions) {
        if (!site.servesTranslatedContent()) {
            return englishOptions;
        }
        return englishOptions.stream()
                .map(english -> {
                    // The one string deliberately left untranslated falls back to the base language
                    // for Arabia (Survey UC-009 BR-005); everything around it is translated.
                    String translated = translations.option(site.languageTag(), english);
                    boolean served = translated != null
                            && !(ARABIC.equals(site.languageTag()) && UNTRANSLATED_IN_ARABIC.equals(english));
                    return served ? translated : english;
                })
                .toList();
    }

    /** Logs in, walks the whole survey in the site's language, finishes, logs out. */
    private void takeWholeSurvey(Site site, String label, String expectedRaceWording) {
        visit(page -> {
            SectionPage section = respondentLogin(page, site, label);
            Recording.beat(site, label + " answers the whole survey in " + site.languageName());
            CensusHouseholdSurvey.Answers answers = new CensusHouseholdSurvey.Answers(label);
            Set<String> done = CensusHouseholdSurvey.startAndReachRaceSection(section, answers);
            assertRaceSectionReads(site, section, expectedRaceWording);
            CensusHouseholdSurvey.walkToReview(section, answers, done);
            ReviewPage review = new ReviewPage(section.page());
            List<String> titles = review.sectionTitles();
            // The per-person step's section title carries the name typed for that person, in the
            // section name's own translation ({name|this person} -> "{name|esta persona}").
            CensusHouseholdSurvey.assertReviewLists(titles,
                    List.of(answers.personText(1), answers.personText(2)));
            review.finish();
            openSurvey(page, site, "/logout");
        });
    }

    /** Logs in, answers as far as the Race and language section, reads it, and leaves unfinished. */
    private void startAndPause(Site site, String label, String expectedRaceWording) {
        visit(page -> {
            SectionPage section = respondentLogin(page, site, label);
            Recording.beat(site, label + " gets as far as Race and language, then leaves unfinished");
            CensusHouseholdSurvey.startAndReachRaceSection(section, new CensusHouseholdSurvey.Answers(label));
            assertRaceSectionReads(site, section, expectedRaceWording);
            openSurvey(page, site, "/logout");
        });
    }

    /** Logs back in, walks on from wherever the respondent left off, and finishes. */
    private void resumeAndFinish(Site site, String label, String expectedRaceWording) {
        visit(page -> {
            SectionPage section = respondentLogin(page, site, label);
            Recording.beat(site, label + " resumes — and reads the wording their first visit was anchored to");
            CensusHouseholdSurvey.Answers answers = new CensusHouseholdSurvey.Answers(label);
            Set<String> done = CensusHouseholdSurvey.startAndReachRaceSection(section, answers);
            // Survey UC-009 BR-010: still the wording this respondent's first access was anchored to.
            assertRaceSectionReads(site, section, expectedRaceWording);
            CensusHouseholdSurvey.walkToReview(section, answers, done);
            new ReviewPage(section.page()).finish();
            openSurvey(page, site, "/logout");
        });
    }

    /** The race question and its options, as this site's respondents should read them. */
    private void assertRaceSectionReads(Site site, SectionPage section, String englishWording) {
        CensusHouseholdSurvey.assertRaceQuestionReads(section, asRead(site, englishWording));
        CensusHouseholdSurvey.assertRaceOptionsRead(section, raceOptionsAt(site));
        // The neighboring multi-select shows the same fallback story: in Arabic every option but
        // the untranslated one is Arabic.
        assertEquals(optionsAt(site, LANGUAGE_OPTIONS),
                section.optionLabels(section.keyEndingWith(CensusHouseholdSurvey.languages())),
                site + ": the languages question's options");
    }

    private void assertStatus(Site site, Map<String, String> expected) {
        visit(page -> {
            adminLogin(page, site);
            assertNoBlockingDialog(page, site);
            Recording.beat(site, "Search: each respondent's status");
            openAdmin(page, site, "/");
            SearchPage search = new SearchPage(page);
            List<String> problems = new ArrayList<>();
            for (Map.Entry<String, String> e : expected.entrySet()) {
                search.searchByAccessCode(codes.get(e.getKey()));
                String actual = search.rowCount() == 1 ? search.statusAt(0) : "(" + search.rowCount() + " rows)";
                if (!e.getValue().equals(actual)) {
                    problems.add(e.getKey() + ": " + actual + ", expected " + e.getValue());
                }
            }
            if (!problems.isEmpty()) {
                fail("status on " + site + ":\n" + String.join("\n", problems));
            }
        });
    }

    private void exportRespondents(Site site, String... labels) {
        visit(page -> {
            adminLogin(page, site);
            assertNoBlockingDialog(page, site);
            openAdmin(page, site, "/");
            SearchPage search = new SearchPage(page);
            for (String label : labels) {
                Recording.beat(site, "exporting " + label + " (ELICIT_EXPORT_V2, one file per respondent)");
                search.searchByAccessCode(codes.get(label));
                assertEquals(1, search.rowCount(), "expected one row for " + label);
                Path file = search.exportRespondent(codes.get(label),
                        Files.createDirectories(exportDir().resolve(site.name().toLowerCase())));
                String text = Files.readString(file, StandardCharsets.UTF_8);
                assertTrue(text.startsWith("# ELICIT_EXPORT_V2"), label + ": unexpected export header:\n" + headerOf(text));
                assertTrue(text.contains("# survey_key: " + surveyKey), label + ": the export should name the survey by key");
                remoteExports.put(label, file);
            }
        });
    }

    private Site siteOf(String label) {
        if (label.startsWith("usa")) {
            return USA;
        }
        return label.startsWith("mex") ? MEXICO : ARABIA;
    }

    private static String headerOf(String text) {
        return text.lines().filter(l -> l.startsWith("#")).collect(Collectors.joining("\n"));
    }

    /** The {@code answers:} and {@code dependents:} lines of a respondent export, in file order. */
    private static List<String> dataLines(String text) {
        return text.lines().filter(l -> l.startsWith("answers: ") || l.startsWith("dependents: ")).toList();
    }
}
