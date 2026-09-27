package com.elicitsoftware.e2e.multilingual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The translator's side of the hand-off process (Author UC-045, UC-046).
 *
 * <p>Author exports one JSON document per survey and language carrying every translatable string
 * with its context, tokens and length budget, and imports the same document back with
 * {@code translation} filled in. The suite plays the translator: this class holds the finished
 * Spanish and Arabic translations of {@code samples/census-household-survey.elicit} as a fixture
 * and fills them into whatever document Author has just exported.</p>
 *
 * <p>The fixture is keyed {@code element_type|field|source_text}, not by element key: it is a
 * dictionary of strings, so it stays readable, survives a re-import of the sample under fresh
 * local ids, and covers the same English string wherever it appears. The hash and the element keys
 * come from Author's own document every time, which is what keeps an imported translation current
 * (UC-046 BR-002) instead of arriving stale from a stale fixture.</p>
 *
 * <p>The fixture must cover the survey completely: {@link #fill} reports every item it could not
 * translate, and the journey fails on a non-empty report rather than importing a partial language.
 * That is the gate that catches a sample the fixture has not kept up with.</p>
 */
final class ContentTranslations {

    static final String FIXTURE = "/fixtures/census-household-survey.translations.json";
    static final String FORMAT = "ELICIT_CONTENT_TRANSLATION_V1";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** language tag -> "element_type|field|source" -> translation. */
    private final Map<String, Map<String, String>> byLanguage;

    private ContentTranslations(Map<String, Map<String, String>> byLanguage) {
        this.byLanguage = byLanguage;
    }

    static ContentTranslations load() {
        try (InputStream in = ContentTranslations.class.getResourceAsStream(FIXTURE)) {
            if (in == null) {
                throw new IllegalStateException("The translation fixture " + FIXTURE + " is not on the test classpath");
            }
            JsonNode root = MAPPER.readTree(in);
            Map<String, Map<String, String>> languages = new LinkedHashMap<>();
            root.fieldNames().forEachRemaining(tag -> {
                if (tag.startsWith("_")) {
                    return; // the fixture's own comment
                }
                Map<String, String> strings = new LinkedHashMap<>();
                root.get(tag).fields().forEachRemaining(e -> strings.put(e.getKey(), e.getValue().asText()));
                languages.put(tag, strings);
            });
            return new ContentTranslations(languages);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The language tags the fixture translates into. */
    List<String> languages() {
        return List.copyOf(byLanguage.keySet());
    }

    /**
     * The fixture's translation of one string, or null when it has none.
     *
     * @param language    the BCP-47 tag
     * @param elementType the table the string lives in, as the hand-off document names it
     * @param field       the column
     * @param source      the English text
     */
    String translationOf(String language, String elementType, String field, String source) {
        Map<String, String> strings = byLanguage.get(language);
        return strings == null ? null : strings.get(elementType + "|" + field + "|" + source);
    }

    /** A question's text in {@code language}, the commonest lookup the journey makes. */
    String questionText(String language, String englishText) {
        return translationOf(language, "questions", "text", englishText);
    }

    /** A select item's label in {@code language}. */
    String option(String language, String englishText) {
        return translationOf(language, "select_items", "display_text", englishText);
    }

    /**
     * A question's short text in {@code language}.
     *
     * <p>Short text is an authoring label almost everywhere, but a MODAL question's short text is
     * what the respondent reads as the dialog header, so on a translated site it has to arrive
     * translated like any other content string.</p>
     */
    String shortText(String language, String englishShortText) {
        return translationOf(language, "questions", "short_text", englishShortText);
    }

    /** A section's name in {@code language}. */
    String sectionName(String language, String englishName) {
        return translationOf(language, "sections", "name", englishName);
    }

    /** What filling a hand-off document produced. */
    record Filled(Path file, int items, int translated, List<String> uncovered) {

        /** True when every item of the document got a translation. */
        boolean isComplete() {
            return uncovered.isEmpty();
        }

        String report() {
            return items + " items, " + translated + " translated"
                    + (uncovered.isEmpty() ? "" : ", no fixture entry for:\n  " + String.join("\n  ", uncovered));
        }
    }

    /**
     * Fills the fixture's translations into the hand-off document Author exported, writing the
     * result beside it as {@code <name>.filled.json} -- the file the journey then uploads.
     *
     * <p>Every other field is left exactly as Author wrote it, as the document's own instructions
     * require: items are neither added, removed nor reordered, and {@code source_hash} travels back
     * untouched so Author can tell whether the base text moved while the file was out (UC-046 A2).</p>
     *
     * @param requestFile the document downloaded from the Translations page
     * @param language    the tag the document must be for
     * @return the filled file and what it covered
     */
    Filled fill(Path requestFile, String language) {
        try {
            JsonNode root = MAPPER.readTree(Files.readString(requestFile, StandardCharsets.UTF_8));
            if (!FORMAT.equals(root.path("format").asText())) {
                throw new IllegalStateException(requestFile + " does not carry " + FORMAT);
            }
            if (!language.equals(root.path("language").asText())) {
                throw new IllegalStateException(requestFile + " is for " + root.path("language").asText()
                        + ", not " + language);
            }
            int items = 0;
            int translated = 0;
            List<String> uncovered = new ArrayList<>();
            for (JsonNode item : root.path("items")) {
                items++;
                String elementType = item.path("element_type").asText();
                String field = item.path("field").asText();
                String source = item.path("source").asText();
                String value = translationOf(language, elementType, field, source);
                if (value == null) {
                    uncovered.add(elementType + "." + field + ": " + shorten(source));
                    continue;
                }
                ((ObjectNode) item).put("translation", value);
                translated++;
            }
            String name = requestFile.getFileName().toString().replaceFirst("\\.json$", "") + ".filled.json";
            Path out = requestFile.resolveSibling(name);
            Files.write(out, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(root));
            return new Filled(out, items, translated, uncovered);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The same as {@link #fill}, but leaving one named item untranslated -- used to show that an
     * item left empty is skipped rather than stored as a blank (UC-046 step 5), and that the
     * respondent then reads that one string in the survey's base language (Survey UC-009 A5).
     */
    Filled fillExcept(Path requestFile, String language, String skipSource) {
        Filled all = fill(requestFile, language);
        try {
            JsonNode root = MAPPER.readTree(Files.readString(all.file(), StandardCharsets.UTF_8));
            int translated = 0;
            for (JsonNode item : root.path("items")) {
                if (skipSource.equals(item.path("source").asText())) {
                    ((ObjectNode) item).put("translation", "");
                } else if (!item.path("translation").asText().isBlank()) {
                    translated++;
                }
            }
            Files.write(all.file(), MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(root));
            return new Filled(all.file(), all.items(), translated, all.uncovered());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String shorten(String text) {
        return text == null ? "null" : text.length() <= 70 ? text : text.substring(0, 67) + "...";
    }
}
