package com.elicitsoftware.e2e.multilingual;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Video;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Records the journey as a video, with a pointer and a caption, and leaves a manifest that
 * {@code make-recording.py} assembles into one narrated film.
 *
 * <p><strong>Off unless asked for.</strong> Everything here is inert without
 * {@code -De2e.record=true}: the ordinary {@code mvn -DskipTests=false test} run records nothing,
 * creates no contexts differently and pays nothing. That matters because the journey is a test
 * first -- it must not become slower or more fragile for the sake of a demo.</p>
 *
 * <h2>The three pieces</h2>
 * <ul>
 *   <li><strong>Video.</strong> Playwright records one webm per {@link BrowserContext}, and this
 *       suite already opens a fresh context per persona visit (see
 *       {@link MultilingualTestBase#visit}). So the clips fall out of the journey's own structure:
 *       one clip is one visit by one person to one site, which is exactly the unit a viewer can
 *       follow. The dead time between visits -- logging in, Maven thinking -- is simply not in any
 *       clip, so the assembled film is tighter than the run was.</li>
 *   <li><strong>Pointer.</strong> {@code recording/overlay.js}, injected into every document. It
 *       needs no cooperation from the page objects: CDP mouse input arrives in the page as real
 *       events, so the overlay can draw the cursor and the click ring from what it observes. See
 *       that file for why it lives in a closed shadow root.</li>
 *   <li><strong>Beats and captions.</strong> {@link #phase} and {@link #beat} do two things at
 *       once: push the words into the overlay so they are burned into the picture, and log them
 *       with a timestamp so the assembler can cut title cards, chapter marks and a WebVTT track at
 *       the right places.</li>
 * </ul>
 *
 * <h2>slowMo is not a luxury here</h2>
 * <p>A recording run launches Chromium with {@code slowMo} ({@value #SLOW_MO_DEFAULT} ms by
 * default, {@code -De2e.record.slowmo=} to change it). Without it Playwright presses the button in
 * the same millisecond it moves the mouse there, and the cursor -- which is animated, because a
 * teleporting cursor reads as a cut rather than as a hand -- is still in flight when the page has
 * already changed. The slowMo has to be longer than the overlay's travel time, not merely
 * non-zero. It also makes a Vaadin application legible: a form that fills instantly shows a viewer
 * nothing.</p>
 *
 * <p>Timings in the manifest are wall-clock milliseconds. The assembler does not trust them as
 * video positions -- a screencast drifts from wall time -- it uses them as fractions of the clip
 * they fall in, against the duration ffprobe reports.</p>
 */
final class Recording {

    /** Long enough for the overlay's pointer to arrive before the click lands. */
    static final int SLOW_MO_DEFAULT = 180;

    private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("e2e.record", "false"));
    private static final int TITLE_CARD_SECONDS = 3;
    /** How long {@link #hold} keeps a page on screen that the journey only reads. */
    private static final int HOLD_DEFAULT = 1800;

    private static final ObjectMapper JSON = new ObjectMapper();

    private static Path dir;
    private static Path videoDir;
    private static String initScript;
    private static long wallStart;

    private static final List<Map<String, Object>> phases = new ArrayList<>();
    private static final List<Map<String, Object>> beats = new ArrayList<>();
    private static final List<Map<String, Object>> clips = new ArrayList<>();

    private static int phaseNumber;
    private static String phaseId = "";
    private static String phaseNarration = "";
    private static String currentBeat = "";
    /** How many phases the story has, for the "3 / 16" chip; see {@link #expectPhases}. */
    private static int totalPhases;

    /** The page of the visit in progress, so a beat can reach the picture it belongs to. */
    private static Page live;
    private static long liveStart;
    private static Video liveVideo;
    private static int livePhase;

    private Recording() {
    }

    static boolean enabled() {
        return ENABLED;
    }

    /** This run's output directory, or null when not recording. For RecordingHarnessSelfTest. */
    static Path directory() {
        return dir;
    }

    // ---- lifecycle ---------------------------------------------------------------------------

    /** Creates {@code target/recording/<stamp>/} and reads the overlay. Call once, before launch. */
    static void start() {
        if (!ENABLED) {
            return;
        }
        try {
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            dir = Files.createDirectories(Path.of(System.getProperty("e2e.record.dir",
                    Path.of("target", "recording", stamp).toString())));
            videoDir = Files.createDirectories(dir.resolve("clips"));
            initScript = overlay();
            wallStart = System.currentTimeMillis();
            System.out.println("[recording] " + dir.toAbsolutePath());
        } catch (IOException e) {
            throw new UncheckedIOException("cannot prepare the recording directory", e);
        }
    }

    /** The slowMo a recording run needs; zero when not recording. */
    static double slowMo() {
        return ENABLED ? Double.parseDouble(System.getProperty("e2e.record.slowmo",
                String.valueOf(SLOW_MO_DEFAULT))) : 0;
    }

    static void decorate(BrowserType.LaunchOptions options) {
        if (ENABLED) {
            options.setSlowMo(slowMo());
        }
    }

    /** Points a new context's video at this run's directory, at the viewport's own size. */
    static void decorate(Browser.NewContextOptions options, int width, int height) {
        if (ENABLED) {
            options.setRecordVideoDir(videoDir).setRecordVideoSize(width, height);
        }
    }

    /** Injects the overlay into every document this context will open. */
    static void attach(BrowserContext context) {
        if (ENABLED) {
            context.addInitScript(initScript);
        }
    }

    /**
     * Adopts the page of a visit that is starting: remembers its clip and pushes the current phase
     * and beat, both now and after every document load. Vaadin's own route changes need no push --
     * the overlay survives them and re-reads the URL itself -- but a hard navigation (into Keycloak
     * and back out of it) builds a new document with an empty caption.
     */
    static void opened(Page page) {
        if (!ENABLED) {
            return;
        }
        live = page;
        liveStart = System.currentTimeMillis();
        liveVideo = page.video();
        livePhase = phaseNumber;
        page.onDOMContentLoaded(p -> push());
        push();
    }

    /**
     * Ends the clip and closes the context. The two are one call because the order matters: the
     * clip's end is the last moment anything was on screen, and the webm is only flushed to disk
     * when the context closes.
     */
    static void closeContext(BrowserContext context, Page page) {
        if (!ENABLED) {
            context.close();
            return;
        }
        long end = System.currentTimeMillis();
        String url = safeUrl(page);
        context.close();                        // flushes the video
        Path file = null;
        try {
            if (liveVideo != null) {
                file = liveVideo.path();
            }
        } catch (RuntimeException e) {
            file = null;                        // a context that never opened a document
        }
        if (file != null && Files.exists(file)) {
            Map<String, Object> clip = new LinkedHashMap<>();
            clip.put("file", relative(file));
            clip.put("phase", livePhase);
            clip.put("start", liveStart - wallStart);
            clip.put("end", end - wallStart);
            clip.put("lastUrl", url);
            clips.add(clip);
        }
        live = null;
        liveVideo = null;
    }

    /** Writes {@code manifest.json}. Call once, after the last phase. */
    static void finish() {
        if (!ENABLED) {
            return;
        }
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("title", "One survey, translated once, read in three languages");
        manifest.put("subtitle", "Elicit multilingual multi-site journey");
        manifest.put("recordedAt", ZonedDateTime.now().toString());
        manifest.put("slowMoMs", slowMo());
        manifest.put("titleCardSeconds", TITLE_CARD_SECONDS);
        manifest.put("viewport", Map.of("width", MultilingualTestBase.VIEWPORT_WIDTH,
                "height", MultilingualTestBase.VIEWPORT_HEIGHT));
        manifest.put("sites", siteCards());
        manifest.put("totalPhases", totalPhases);
        manifest.put("phases", phases);
        manifest.put("beats", beats);
        manifest.put("clips", clips);
        try {
            Path file = dir.resolve("manifest.json");
            JSON.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), manifest);
            System.out.println("[recording] " + clips.size() + " clips, " + beats.size() + " beats -> "
                    + file.toAbsolutePath());
            System.out.println("[recording] assemble with: ./make-recording.py " + relativeToModule(dir));
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write the recording manifest", e);
        }
    }

    // ---- narration ---------------------------------------------------------------------------

    /**
     * How many phases the story will have. Declared by the journey, because only the journey knows
     * -- the caption has to read "1 / 16" while the first phase runs, long before sixteen have
     * been seen. Without it the chip counts up as it goes ("1 / 1", "2 / 2"), which is honest but
     * tells a viewer nothing about how much is left.
     */
    static void expectPhases(int total) {
        if (ENABLED) {
            totalPhases = total;
        }
    }

    /**
     * A phase of the story begins. {@code id} is the journey's own short name for it (the one that
     * appears in a skip message); {@code narration} is the sentence a viewer is shown.
     */
    static void phase(int number, String id, String narration) {
        if (!ENABLED) {
            return;
        }
        phaseNumber = number;
        phaseId = id;
        phaseNarration = narration;
        currentBeat = "";
        totalPhases = Math.max(totalPhases, number);
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("number", number);
        entry.put("id", id);
        entry.put("narration", narration);
        entry.put("at", System.currentTimeMillis() - wallStart);
        phases.add(entry);
        push();
    }

    /**
     * Holds the picture on what is on screen, so a page the journey only has to <em>read</em> is
     * shown long enough to be read. A validation panel or a clean overview is asserted in
     * milliseconds and the clip would otherwise cut the moment it appeared.
     *
     * <p>Inert without {@code -De2e.record=true}: an ordinary run never waits, and no assertion
     * depends on this having happened.</p>
     */
    static void hold(Page page) {
        hold(page, HOLD_DEFAULT);
    }

    /** {@link #hold(Page)} for a given number of milliseconds. */
    static void hold(Page page, int millis) {
        if (ENABLED && page != null) {
            page.waitForTimeout(millis);
        }
    }

    /** A step within the phase, shown on the caption's second line and logged for the subtitles. */
    static void beat(String text) {
        beat(null, text);
    }

    /** The same, attributed to a site -- "Mexico: ..." in the subtitles. */
    static void beat(Site site, String text) {
        if (!ENABLED) {
            return;
        }
        currentBeat = text;
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("phase", phaseNumber);
        entry.put("site", site == null ? null : site.name());
        entry.put("text", text);
        entry.put("at", System.currentTimeMillis() - wallStart);
        beats.add(entry);
        push();
    }

    private static void push() {
        Page page = live;
        if (page == null) {
            return;
        }
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("phase", phaseId);
        state.put("narration", phaseNarration);
        state.put("number", phaseNumber);
        state.put("total", totalPhases);
        state.put("beat", currentBeat);
        try {
            page.evaluate("s => window.__elicitRec && window.__elicitRec.update(s)", state);
        } catch (RuntimeException ignored) {
            // Mid-navigation, or the page is gone: the next load's push catches up.
        }
    }

    // ---- the overlay's configuration ----------------------------------------------------------

    /** The overlay with its configuration substituted in (see the placeholder in overlay.js). */
    private static String overlay() throws IOException {
        try (InputStream in = Recording.class.getResourceAsStream("/recording/overlay.js")) {
            if (in == null) {
                throw new IOException("recording/overlay.js is not on the test classpath");
            }
            String source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            String config = JSON.writeValueAsString(Map.of(
                    "sites", overlaySites(),
                    "extras", List.of(Map.of("url", keycloakBaseUrl(), "name", "Sign in"))));
            return source.replace("\"__ELICIT_RECORDER_CONFIG__\"", config);
        }
    }

    private static List<Map<String, Object>> overlaySites() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SiteBrand brand : BRANDS) {
            Site site = brand.site();
            List<Map<String, String>> apps = new ArrayList<>();
            apps.add(Map.of("url", site.surveyBaseUrl(), "name", "Survey"));
            apps.add(Map.of("url", site.adminBaseUrl(), "name", "Admin"));
            if (site.hasAuthor()) {
                apps.add(Map.of("url", site.authorBaseUrl(), "name", "Author"));
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", site.name());
            entry.put("color", brand.color());
            entry.put("mark", dataUri(brand.mark()));
            entry.put("apps", apps);
            out.add(entry);
        }
        return out;
    }

    /**
     * Each site's own header mark, and the one color that stands for it.
     *
     * <p>The badge wears the mark the page itself wears, which is the whole point of giving the
     * three sites separate brands (see README, "What makes a site look like itself"): three
     * consoles running the same application are otherwise indistinguishable in a recording. Mexico
     * and Arabia both fly a green flag -- their primaries are #006847 and #006C35 -- so color
     * alone would not tell them apart, and the mark does.</p>
     */
    private record SiteBrand(Site site, String color, Path mark) {
    }

    private static final List<SiteBrand> BRANDS = List.of(
            new SiteBrand(Site.usa(), "#1B629C", Path.of("..", "elicit-brand", "images", "icon-white.png")),
            new SiteBrand(Site.mexico(), "#006847", Path.of("mexico", "mexico_brand", "images", "icon-white.png")),
            new SiteBrand(Site.arabia(), "#006C35", Path.of("arabia", "arabia_brand", "images", "icon-white.png")));

    /** The brand marks as the assembler needs them: name, color and mark file, for the cards. */
    private static List<Map<String, Object>> siteCards() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SiteBrand brand : BRANDS) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", brand.site().name());
            entry.put("color", brand.color());
            entry.put("language", brand.site().languageTag() == null ? "en" : brand.site().languageTag());
            entry.put("rightToLeft", brand.site().rightToLeft());
            entry.put("mark", brand.mark().toString());
            out.add(entry);
        }
        return out;
    }

    /**
     * A file:// image is not loadable as a subresource of an application page, so the mark travels
     * inlined -- the same reason make-brand-images.py inlines the flag artwork it renders.
     */
    private static String dataUri(Path png) {
        try {
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(png));
        } catch (IOException e) {
            return "";                          // the badge falls back to its flat color
        }
    }

    private static String keycloakBaseUrl() {
        return System.getProperty("keycloak.baseUrl", "http://localhost:8180");
    }

    // ---- odds and ends -----------------------------------------------------------------------

    private static String safeUrl(Page page) {
        try {
            return page.url();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static String relative(Path file) {
        Path absolute = file.toAbsolutePath();
        Path base = dir.toAbsolutePath();
        return absolute.startsWith(base) ? base.relativize(absolute).toString() : absolute.toString();
    }

    private static String relativeToModule(Path path) {
        Path cwd = Path.of("").toAbsolutePath();
        Path absolute = path.toAbsolutePath();
        return absolute.startsWith(cwd) ? cwd.relativize(absolute).toString() : absolute.toString();
    }
}
