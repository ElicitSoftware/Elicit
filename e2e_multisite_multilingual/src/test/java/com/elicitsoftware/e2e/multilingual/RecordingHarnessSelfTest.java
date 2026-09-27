package com.elicitsoftware.e2e.multilingual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Route;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Proves the recording harness works, without the three stacks.
 *
 * <p>The journey it exists for takes a quarter of an hour against three running sites, which is no
 * way to find out that the pointer overlay is not attaching or that the assembler mis-joins its
 * segments. This exercises every piece of {@link Recording} -- the injected overlay, the pointer's
 * own view of the clicks, the caption push, the per-visit clip, the manifest, and
 * {@code make-recording.py} itself -- against a page served out of the test.</p>
 *
 * <p>Served, not loaded from disk: the page is fulfilled by a Playwright route at each site's real
 * base URL, so no server is needed and the overlay's URL-to-site resolution is exercised for what
 * it is. A {@code file://} page would have told us nothing about the badge.</p>
 *
 * <p>Not part of the journey's suite -- surefire here runs {@code *E2ETest} only. Run it by name:</p>
 *
 * <pre>
 * mvn -DskipTests=false -De2e.record=true -Dtest=RecordingHarnessSelfTest test
 * </pre>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RecordingHarnessSelfTest extends MultilingualTestBase {

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * A stand-in for a section page: a heading, fields to focus, options to tick and a Next button,
     * spread to the corners of the viewport so the pointer has somewhere to travel.
     */
    private static final String DEMO_PAGE = """
            <!DOCTYPE html><html lang="en"><head><meta charset="utf-8"><title>Harness demo</title>
            <style>
              html,body{margin:0;height:100%;background:#FDFCFA;color:#1A2229;
                font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Helvetica,sans-serif}
              header{background:#1B629C;color:#fff;padding:18px 34px;font-size:20px;font-weight:600}
              main{padding:40px 34px;max-width:900px}
              h1{font-size:30px;margin:0 0 6px}
              p.lead{color:#4A5A6A;margin:0 0 30px}
              label{display:block;margin:14px 0;font-size:17px}
              input[type=text]{font-size:17px;padding:10px 12px;width:360px;
                border:1px solid #C9C3B7;border-radius:6px}
              .row{display:flex;gap:14px;margin-top:38px}
              button{font-size:17px;padding:12px 22px;border:0;border-radius:6px;
                background:#1B629C;color:#fff;cursor:pointer}
              button.ghost{background:#E8E4DC;color:#1A2229}
              #far{position:fixed;right:40px;top:120px}
              #count{font-variant-numeric:tabular-nums}
            </style></head><body>
            <header id="brandbar">Harness demo console</header>
            <main>
              <h1 id="title">Section <span id="count">1</span></h1>
              <p class="lead">A page with somewhere for the pointer to go.</p>
              <label>Who lives here? <input id="who" type="text"></label>
              <label><input id="opt1" type="checkbox"> White</label>
              <label><input id="opt2" type="checkbox"> Asian</label>
              <label><input id="opt3" type="checkbox"> Some other race or origin</label>
              <div class="row">
                <button id="next">Next</button>
                <button id="back" class="ghost">Previous</button>
              </div>
            </main>
            <button id="far" class="ghost">Far corner</button>
            <script>
              const count = document.getElementById('count');
              document.getElementById('next').onclick =
                  () => count.textContent = String(Number(count.textContent) + 1);
              document.getElementById('back').onclick =
                  () => count.textContent = String(Math.max(1, Number(count.textContent) - 1));
            </script></body></html>
            """;

    private Path runDir;

    @Test
    @Order(1)
    void theOverlayDrawsThePointerAndCarriesTheCaption() {
        assumeTrue(Recording.enabled(),
                "the harness only does anything with -De2e.record=true; see the class javadoc");
        Recording.expectPhases(2);
        Recording.phase(1, "1 self-test", "The harness records a page, a pointer and a caption");

        visit(page -> {
            serve(page);
            Recording.beat(USA, "a page served at the master's Survey URL");
            page.navigate(USA.surveyBaseUrl() + "/section");
            page.locator("#title").waitFor();

            Map<String, Object> before = debug(page);
            assertEquals(Boolean.TRUE, before.get("attached"),
                    "the overlay should have attached itself to the document");
            assertEquals("The harness records a page, a pointer and a caption", before.get("narration"),
                    "the phase narration should have been pushed into the page");
            assertEquals("Survey", before.get("place"),
                    "the overlay should read the master's Survey out of the URL");
            assertEquals(0, ((Number) before.get("clicks")).intValue(), "nothing clicked yet");

            // Deliberately far apart, and in an order that makes the pointer cross the viewport.
            Recording.beat(USA, "clicking, ticking and typing");
            page.click("#far");
            page.click("#opt1");
            page.click("#next");
            page.fill("#who", "Census household");
            page.click("#opt3");
            page.click("#next");

            Map<String, Object> after = debug(page);
            assertEquals(5, ((Number) after.get("clicks")).intValue(),
                    "the overlay should have seen every click Playwright made");
            assertTrue(((Number) after.get("x")).doubleValue() > 0
                            && ((Number) after.get("y")).doubleValue() > 0,
                    "the pointer should be somewhere in the page: " + after);
            assertEquals("clicking, ticking and typing", after.get("beat"),
                    "the beat should be on screen");
            assertEquals("3", page.locator("#count").innerText(),
                    "the demo page should have advanced, i.e. the overlay stole no clicks");
        });
    }

    /** The same page at Mexico and Arabia: a second clip, a second phase, other badges. */
    @Test
    @Order(2)
    void everySiteIsRecognizedFromItsUrl() {
        assumeTrue(Recording.enabled(), "needs -De2e.record=true");
        Recording.phase(2, "2 badges", "Each site is named from the URL of the page being recorded");

        for (Site site : List.of(MEXICO, ARABIA)) {
            visit(page -> {
                serve(page);
                Recording.beat(site, "served at " + site.adminBaseUrl() + ", so the badge says "
                        + site.name() + " / Admin");
                page.navigate(site.adminBaseUrl() + "/departments");
                page.locator("#title").waitFor();
                assertEquals("Admin", debug(page).get("place"),
                        site + ": the overlay should read Admin out of " + site.adminBaseUrl());
                page.click("#next");
                page.click("#opt2");
            });
        }
    }

    /** The manifest, and then the film the assembler makes out of it. */
    @Test
    @Order(3)
    void theManifestAssemblesIntoAFilm() throws Exception {
        assumeTrue(Recording.enabled(), "needs -De2e.record=true");
        Recording.finish();                     // also run again by @AfterAll; writing it twice is fine
        runDir = Recording.directory();

        Path manifestFile = runDir.resolve("manifest.json");
        assertTrue(Files.exists(manifestFile), "the manifest should have been written");
        JsonNode manifest = JSON.readTree(manifestFile.toFile());

        assertEquals(3, manifest.get("clips").size(), "one clip per visit");
        assertEquals(2, manifest.get("phases").size(), "two phases");
        assertTrue(manifest.get("beats").size() >= 4, "the beats should have been logged");
        assertTrue(manifest.get("slowMoMs").asDouble() >= Recording.SLOW_MO_DEFAULT,
                "a recording run needs a slowMo longer than the pointer's travel");
        for (JsonNode clip : manifest.get("clips")) {
            Path video = runDir.resolve(clip.get("file").asText());
            assertTrue(Files.exists(video) && Files.size(video) > 1024,
                    "a clip should be a real webm: " + video);
            assertTrue(clip.get("end").asLong() > clip.get("start").asLong(),
                    "a clip should span some time: " + clip);
        }
        assertEquals(3, manifest.get("sites").size(), "all three sites' brand marks");
        for (JsonNode site : manifest.get("sites")) {
            assertTrue(Files.exists(Path.of(site.get("mark").asText())),
                    site.get("name").asText() + "'s brand mark should exist: " + site.get("mark"));
        }

        assumeTrue(onPath("ffmpeg") && onPath("ffprobe"), "no ffmpeg: the assembly is not checked");
        int status = assemble();
        assertEquals(0, status, "make-recording.py should succeed; its output is above");

        Path film = runDir.resolve("journey.mp4");
        assertTrue(Files.exists(film) && Files.size(film) > 10_000, "the film should exist: " + film);
        assertTrue(Files.exists(runDir.resolve("journey.vtt")), "the subtitles should exist");
        assertTrue(Files.exists(runDir.resolve("journey.png")), "the poster should exist");
        // Three clips, three phase/opening/closing cards: comfortably more than ten seconds.
        assertTrue(duration(film) > 10, "the film is suspiciously short: " + duration(film) + "s");
        assertTrue(Files.readString(runDir.resolve("journey.vtt"), StandardCharsets.UTF_8)
                        .contains("clicking, ticking and typing"),
                "a beat should have become a subtitle cue");
        System.out.println("[self-test] " + film.toAbsolutePath() + " (" + duration(film) + "s)");
    }

    // ---- plumbing ----------------------------------------------------------------------------

    /**
     * Answers every request in this page with the demo page. No server, and the page still arrives
     * over http at a URL the overlay has to recognize.
     */
    private static void serve(Page page) {
        page.route("**/*", (Route route) -> route.fulfill(new Route.FulfillOptions()
                .setStatus(200)
                .setContentType("text/html; charset=utf-8")
                .setBody(DEMO_PAGE)));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> debug(Page page) {
        return (Map<String, Object>) page.evaluate("() => window.__elicitRec.debug()");
    }

    private int assemble() throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("./make-recording.py", runDir.toString(),
                "--card-seconds", "1.5")
                .redirectErrorStream(true)
                .directory(Path.of("").toAbsolutePath().toFile());
        Process process = builder.start();
        try (var out = process.getInputStream()) {
            System.out.write(out.readAllBytes());
            System.out.flush();
        }
        assertTrue(process.waitFor(5, TimeUnit.MINUTES), "make-recording.py did not finish");
        return process.exitValue();
    }

    private static double duration(Path file) throws IOException, InterruptedException {
        Process probe = new ProcessBuilder("ffprobe", "-v", "error", "-show_entries",
                "format=duration", "-of", "default=nw=1:nk=1", file.toString()).start();
        String out = new String(probe.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        probe.waitFor(30, TimeUnit.SECONDS);
        try {
            return Double.parseDouble(out);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean onPath(String tool) {
        for (String entry : System.getenv().getOrDefault("PATH", "").split(":")) {
            if (Files.isExecutable(Path.of(entry, tool))) {
                return true;
            }
        }
        return false;
    }
}
