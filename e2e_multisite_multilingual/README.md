# Multilingual multi-site end-to-end test

Tests the theory of **one survey, translated once, read in three languages at three sites**: a
survey is authored and translated at a master site, the one definition file it exports is applied
everywhere, and each site serves the survey in whichever of its languages that site has adopted —
while every respondent, whatever their language, ends up in the master's database.

Three complete stacks run side by side on this machine:

| | USA (master) | Mexico | Arabia |
|---|---|---|---|
| Survey | http://localhost:8080 | http://localhost:8030 | http://localhost:7980 |
| Admin | http://localhost:8081 | http://localhost:8031 | http://localhost:7981 |
| Author | http://localhost:8084 (preview 8085) | — | — |
| Keycloak | http://localhost:8180 (admin/admin) | uses USA's | uses USA's |
| Mailpit | http://localhost:8025 | uses USA's | uses USA's |
| PostgreSQL | localhost:5452 | localhost:5402 | localhost:5352 |
| Translations mount | `i18n/usa` — **nothing** | `i18n/mexico` — `es-419` | `i18n/arabia` — `ar` |
| Brand mount | `../elicit-brand` — the default | `mexico/mexico_brand` | `arabia/arabia_brand` |
| What a respondent reads | English | Latin American Spanish | Arabic, right to left |

Mexico's host ports are USA's minus 50, Arabia's minus 100. Neither remote site publishes SMTP, and
none of the three runs FHHS, Pedigree or Jaeger: FHHS serves only the Family History Survey and
would never become ready against this one (FHHS UC-005), Pedigree is used by nothing but FHHS, and
this suite reads its failures from screenshots and container logs rather than from traces. The
remote sites share USA's **Keycloak** (the realm lists `http://localhost:8031/*` and
`http://localhost:7981/*` as Admin redirect URIs) and its **Mailpit** — their Admin containers also
join USA's compose network, so the service name `mailpit` resolves there. Being on two networks
makes the shared names `survey` and `db` ambiguous for those Admins, so each remote site's own
services carry the aliases `mexico-survey` / `mexico-db` and `arabia-survey` / `arabia-db`, and
Admin is pointed at those. All three Admins sign in as `admin/admin`; each site keeps its own
`survey.users` row and department assignments.

Only USA has Author, and only USA has an `author` database.

## What makes a site look like itself

Each site mounts its own brand at `/opt/brand`, so the three are told apart on
sight rather than by reading the port in the address bar. That matters for
screenshots and for the recording of the journey ("Recording the journey"
below, where each site's badge wears its own mark): three consoles running the
same application are otherwise identical, and the brand is the only thing on
screen that says which one this is.

| | USA (master) | Mexico | Arabia |
|---|---|---|---|
| Header | Ink navy | Bandera green, red rule | flag green |
| Mark | the Elicit clipboard | the flag of Mexico | the flag of Arabia |
| Background | Paper | Paper | Sand |
| Type | system stack | system stack | Arabic-first stack, looser leading |

The two site brands are complete brand packages, not color overrides: each
carries every file `BrandDiagnostics` expects (`theme.css`, the two
stylesheets, `HorizontalLogo.png`, `icon-white.png`, `favicon.ico`) and every
`--brand-*` token the default brand defines, so nothing falls through to a
Lumo default and looks half-themed. Admin ▸ System Branding (UC-026) at each
site lists them and flags anything missing.

Both palettes come from the site's flag, and both were contrast-checked rather
than eyeballed — Arabia's gold in particular is unusable at its flag value
both as a fill under white text and as text on Sand, so it is split into a
Brass fill and a darker Gold text color. Arabia's brand is also the only one
built for right-to-left: it names Arabic faces ahead of Latin ones, opens the
line height, and pins the access-code pill to `direction: ltr` so an RTL
paragraph does not reorder the code. Each brand's own `README.md` has the
reasoning; `arabia_brand/README.md` also says why "Arabia" flies an invented
flag rather than a real nation's.

The images are generated, not hand-drawn. `make-brand-images.py` renders every
PNG and the `.ico` from the one `images/flag.svg` in each brand, through the
Chromium that Playwright already installed for this suite — which is also what
shapes the Arabic wordmark correctly. Swap in different flag artwork and
re-run it; nothing else needs editing:

```sh
./make-brand-images.py                       # both site brands
./make-brand-images.py mexico/mexico_brand   # just one
```


## What makes a site "Spanish" or "Arabic"

Two independent things have to line up before a respondent reads a translated question, and the
suite exercises both halves separately:

1. **The survey must publish the language.** The author declares the published set in Author
   (UC-044) and translates the content (UC-043, UC-045, UC-046); both the set and every translation
   travel inside the one `.elicit` file. There is no per-site export — one revision is one file
   (UC-044 BR-003).
2. **The site must offer the language for its own screens.** The applications carry every language
   the release supports, and a site names the ones it offers in `i18n.bundled.locales`. A site
   serves a survey
   language only where the site *also* offers that language for the chrome (Survey UC-009 BR-009),
   so no respondent ever reads translated questions between English buttons.

There is nothing to lay out. The applications' own texts are packaged in the images, so each site
just names the ones it offers in `i18n.bundled.locales` — `en` for the master, `en,es-419` for
Mexico, `en,ar` for Arabia. That is still the "application texts" half of distribution: the
definition file carries the survey's content translations, the images carry the applications' own
texts, and they arrive by different routes.

The master offers **English only** on purpose. It therefore hides the language
selector (an English-only deployment) and serves the survey's questions in English even though the
Spanish and Arabic translations are sitting in its own database — which is exactly BR-009, and the
journey asserts it.

## The story the test runs

`CensusMultilingualE2ETest` is one ordered story, one JUnit method per phase; a failed phase marks
the rest skipped so the report shows where the theory broke. Every persona visit (the author, each
site's administrator, every respondent login) runs in a fresh browser context — which matters more
here than in `../e2e_multisite`, because the chosen language lives in the browser session.

1. USA's author **imports** this directory's `census-household-survey.elicit` into Author (UC-005)
   and **fixes it**. That file is the sample of the same name with one deliberate fault (see "The
   definition the master imports" below): the `SHOW` rule that reveals the Rent details section
   points at a section the respondent has already passed. The import succeeds — an import is a load,
   not a review — and the editor's validation panel refuses to call the survey ready, naming the rule
   as pointing backwards (UC-007, UC-033 A3). The author opens the **designer**, where that rule's
   arrow is drawn red and labeled, re-points it at Rent details in the rule dialog (UC-019), and the
   overview then reports "Ready to export: no findings." Any *other* finding fails the phase with the
   panel's own text, so a real problem in the sample still surfaces here rather than three phases on.
2. The author declares the survey **published in `es-419` and `ar`** (UC-044) and opens the
   Translations page, which lists every translatable string of the survey with nothing translated.
3. The author **translates the survey**. One string is typed straight into the grid (UC-043 step 5);
   the rest go out in the hand-off document Author generates (UC-045), are filled in from this
   suite's fixture, and come back through the import (UC-046). The counts are asserted at every
   step: the hand-typed string returns as *unchanged*, not imported; Arabic deliberately leaves one
   answer option empty, which is reported as *left untranslated* and shows up under the page's "only
   what needs work" filter.
4. The author **exports revision 1** (UC-008). Validation warns that Arabic has one untranslated
   string and exports anyway — an incomplete language is a warning, not an error (UC-044 A1). The
   file is asserted to carry the published set, both languages' translations, and the count of them.
5. USA creates its department (Admin UC-028) and **applies** revision 1.
6. Mexico and Arabia create theirs and apply **the same file**. Each remote Survey now offers its
   language in the selector, Arabia's pages are laid out right to left, and USA's offer no choice at
   all. Each remote console has the language too, though this suite drives every console in English.
7. USA registers three subjects. The first finishes the survey in **English**; the second stops on
   the Race and language section; the third never logs in.
8. Mexico does the same in **Spanish** — the question and every answer option asserted against the
   fixture, not just "not English".
9. Arabia does the same in **Arabic**, right to left. The one option that was never translated is
   read in English among Arabic ones: the base-language fallback for a single string (UC-009 A5,
   BR-005).
10. USA's author **rewords** "Which of the following describe you? Select all that apply." to "What
    race do you consider yourself to be? Select all that apply." (UC-011). The designer says at once
    which languages that invalidated (UC-043 A1, FR-060); the Translations page shows one row out of
    date per language; the author **retranslates both by hand** and exports revision 2.
11. USA applies revision 2.
12. On USA, the respondent who was part-way through **resumes and still reads the old wording**
    (their snapshot is anchored to first access, UC-009 BR-010) and finishes; the one who had never
    logged in starts on the **new** wording and finishes.
13. The master shares revision 2; Mexico and Arabia apply it.
14. The same two rules hold at both remote sites, **in their own language**: the paused respondent
    resumes on the old Spanish/Arabic wording, the fresh one starts on the new one, and all finish.
15. Mexico and Arabia **export** their three respondents each, one file per respondent
    (`ELICIT_EXPORT_V2`, Admin UC-011).
16. USA creates a local department carrying each remote site's code and **imports all six**
    (UC-012). Search then shows all nine respondents, the imported six Finished and in the right
    department, and re-exporting one from the master reproduces the remote site's answer and
    dependent lines byte for byte.

## The definition the master imports

`census-household-survey.elicit` here is a copy of `../samples/census-household-survey.elicit` — the
demonstration survey that exercises every question type and every rule the engine supports — with
**one deliberate fault**: relationship 3, the `SHOW` rule that should reveal the *Rent details*
section when the tenure question is answered `RENT`, points at the *About you* section of step 2
instead (`downstream_step_id` 2, `downstream_ss_id` 2). Everything else is the sample byte for byte,
so the translation fixture's keys still match.

It is there because a journey that only ever imports a clean file shows nothing of what Author is
for. This fault is one Author has an answer to at every level — a validation finding that blocks the
export, a red arrow with its reason on the board, and a rule dialog that would refuse to save the
rule back into that state (UC-033) — and fixing it is the second half of phase 1.

To refresh the copy after the sample changes: copy the sample over it and re-apply the fault to the
`relationships` record with id 3 (the recipe is in the file's own header comment). Then

```bash
../samples/validate-elicit.py census-household-survey.elicit
```

which replays Author's own checks and must report **exactly one** error, the backwards rule.

## The translations

`src/test/resources/fixtures/census-household-survey.translations.json` holds the finished Latin
American Spanish and Arabic of all 130 translatable strings, keyed
`element_type|field|source_text`. It is a **dictionary of strings, not a hand-off document**: the
suite downloads the real document from Author every run and fills the dictionary into it, so the
element keys and the `source_hash` of every item are always Author's own. That is what makes an
imported translation land *current* (UC-046 BR-002) rather than arriving stale from a fixture that
has drifted.

The fixture must cover the survey completely: the phase fails, naming every string it could not
translate, rather than importing a partial language. That is the gate that catches a sample the
fixture has not kept up with.

Two tools regenerate and check it:

```sh
src/test/resources/fixtures/list-translatable-strings.py ../samples/census-household-survey.elicit
src/test/resources/fixtures/make-translations.py <keys.json> <out.json>
```

`make-translations.py` holds the translations themselves and re-applies the checks Author's importer
applies — placeholder count, token set, balanced braces, the length budget, and answer options of
one question staying distinguishable from each other — so a bad translation is caught before a run
rather than as a rejection in the middle of one.

The application chrome is **not** fixture data: Mexico and Arabia use the real `es-419` and `ar`
bundles the images ship, which is also where the two navigation captions a respondent's
`SectionPage` needs come from.

## Prerequisites

- Docker Desktop, and the `survey`, `admin` and `author` images built from the umbrella root:
  `./buildDockerImages.sh Survey Admin Author`. Pedigree and FHHS are not needed.
- The umbrella checkout, for `../elicit-brand`, `../keycloak` and `../db-init`.
  The definition itself lives here, not in `../samples` (see "The definition the master imports").
- Playwright's Chromium, installed once:
  `mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"`
  (from `../e2e-tests`, or here after a first `mvn test-compile`).
- To record the journey (see "Recording the journey"), also `ffmpeg` and `ffprobe` on PATH, and
  python3 with Pillow — the same two things `make-brand-images.py` needs.
- The umbrella stack **and** `../e2e_multisite` stopped (`docker compose down` in the repo root,
  `../e2e_multisite/down.sh`): USA uses the same ports as both, and Mexico the same as site 2.

## Running

```bash
./run.sh             # reset all three databases, start all three sites, run the journey
```

or step by step:

```bash
./reset.sh all       # wipe data/ (PGDATA of each site, USA's Mailpit and Keycloak H2 copy)
./up.sh              # lay out the i18n mounts, then USA, Mexico, Arabia in that order
./status.sh          # HTTP probe of every URL above
mvn -DskipTests=false test               # the journey (add -De2e.headless=false to watch)
./down.sh            # stop all three
```

The journey installs the one **Census Household Survey** — a single survey key across both revisions
and all three sites, so the star schema keeps the same dimensions through the update. That is why
the databases are cleared between runs; phase 1 refuses to start if Author already holds the survey.

A whole run takes about fifteen minutes: five to initialize three databases from scratch and about
nine for the journey itself (16 phases, three full walks of a twenty-five-question survey plus six
partial ones, in three languages).

Exported `.elicit` files, the hand-off documents and their filled copies land in
`target/exports/<timestamp>/`; a failing phase writes `target/failure-<time>.png`.

Base URLs can be overridden with `-Dusa.survey.baseUrl=…`, `-Dusa.admin.baseUrl`,
`-Dusa.author.baseUrl`, `-Dmexico.survey.baseUrl`, `-Dmexico.admin.baseUrl`,
`-Darabia.survey.baseUrl`, `-Darabia.admin.baseUrl`; credentials with
`-Dadmin.username/-Dadmin.password/-Dauthor.username/-Dauthor.password`; the definition to import
with `-Dcensus.definition=…`.

## Recording the journey

The journey can record itself, and the recording is the only way anyone who is not going to read
sixteen phases of Java will see the theory demonstrated. It is off unless asked for: an ordinary
`mvn -DskipTests=false test` records nothing and pays nothing for the harness being there.

Needs the same prerequisites as a plain run, plus `ffmpeg` and `ffprobe` on PATH and python3 with
Pillow (the two things `make-brand-images.py` already needs).

### Recording one

```bash
./record.sh                     # reset all three databases, start all three sites, record, assemble
./record.sh --no-reset          # against stacks that are already up and clean
./record.sh --speed 1.0         # the same at life size, rather than a quarter slower
```

`record.sh` is `./reset.sh all`, `./up.sh all`, the journey with the harness on, then the assembler.
Anything on its command line other than `--no-reset` is passed straight to `make-recording.py`.

Recording by hand is the same two steps:

```bash
mvn -DskipTests=false -De2e.record=true test       # writes target/recording/<timestamp>/
./make-recording.py                                # assembles the newest run
```

A recorded run takes **about half an hour**, not fifteen minutes: Chromium is deliberately slowed
down ("slowMo is not a luxury here" below).

### Assembling one

`./make-recording.py` takes the newest run under `target/recording/` unless given a directory, and
can be re-run as often as you like without re-running the journey — the clips are already on disk.

```bash
./make-recording.py                                 # the newest run, 0.75x, with cards
./make-recording.py target/recording/20260927-0113  # a particular run
./make-recording.py --speed 1.0                     # life size; 1.5 to skim, 0.5 to pore over
./make-recording.py --no-cards                      # clips only, no title cards and no chapters
./make-recording.py --out short.mp4 --card-seconds 2
./make-recording.py --help                          # also --fps, --crf, --width/--height, --keep, --verbose
```

**The clips play at `--speed 0.75` by default** — a quarter slower than they were recorded, because
at life size a Vaadin form fills and a page changes faster than a viewer can follow even with the
slowMo of the run itself. Title cards keep their length whatever the speed, and so does the clock the
subtitles run on: every cue is placed as a fraction of its clip, so the `.vtt` follows the picture at
any speed. A full sixteen-phase run comes out about sixteen minutes at 0.75 and twelve at 1.0.

Each `--out` name gets its own `.vtt` and `.png` beside it, so two cuts of one run do not overwrite
each other.

### What you get

In `target/recording/<timestamp>/` (ignored by git, like the rest of `target/`):

| | |
|---|---|
| `journey.mp4` | the film: a title card per phase, then that phase's clips, with chapters |
| `journey.vtt` | the beats as subtitles |
| `journey.png` | a still, for wherever the film is embedded |
| `clips/*.webm` | what Playwright recorded, one per persona visit |
| `manifest.json` | what happened when: phases, beats and clips, in wall-clock milliseconds |

The chapters are real MP4 chapters: QuickTime, VLC and IINA all list them, so a viewer can jump
straight to "9. And at Arabia, in Arabic, right to left". MP4 chapter tracks tile the timeline, so
chapter 1 absorbs the opening card and starts at zero however the metadata was written.

### Changing what it says

Two places, both in `CensusMultilingualE2ETest`:

- **A phase's sentence** is the second argument of `phase(...)`, beside the phase's own short name.
  It is shown on the phase's title card, in the lower third for the whole phase, and as the chapter
  title. It is nothing to the test — changing it cannot break an assertion.
- **A beat** is `Recording.beat(site, "...")`, which sets the caption's second line and becomes a
  subtitle cue. Put one immediately before the thing it describes. Most of them are in the shared
  helpers, so one beat narrates every site that helper runs at; a beat about something that happens
  once goes in that phase.

Both are inert when not recording, so beats can be added freely without slowing the suite down. So
is `Recording.hold(page)`, the third lever: it keeps a page on screen for a moment where the journey
only has to *read* it. A validation panel is asserted in milliseconds and the clip would otherwise cut
the instant it appeared — phase 1 holds on the findings, on the red arrow and on the clean overview.
An ordinary run never waits, and no assertion depends on the hold having happened.

### The three pieces

**Video.** Playwright records one webm per browser context, and this suite already opens a fresh
context per persona visit. So the clips fall out of the journey's own structure without anything
being arranged for them: one clip is one person's visit to one site, which is exactly the unit a
viewer can follow. The dead time between visits is in no clip, so the film is tighter than the run.

**A pointer.** Playwright drives the mouse through CDP, which moves no visible cursor and leaves no
mark where it clicks, so a raw screencast is a page that changes for no reason. `recording/overlay.js`
draws a cursor and a click ring — and it needs no cooperation from the page objects, because CDP
input arrives in the page as ordinary `mousemove` and `mousedown` events at real coordinates. It
listens, in the capture phase, and draws what it sees. A `fill()` sets a value with no keystrokes at
all, so a field that takes focus is ringed too; otherwise text would simply appear.

The overlay lives in a **closed** shadow root. That is not decoration. Playwright's locators pierce
*open* shadow roots, so a caption in one could be matched by a `getByText` and make an assertion
pass or fail on the narration rather than on the page. A closed root cannot be reached by any
locator while still rendering into the video, and the host is `pointer-events: none`, so it is not a
hit-test target and never steals a click.

**Beats and captions.** `Recording.phase` and `Recording.beat` push their words into the overlay —
so they are burned into the picture — and log them with a timestamp, which is what lets
`make-recording.py` cut a title card, a chapter mark and a subtitle cue at the right places. Most
beats sit in the shared helpers (`adminLogin`, `register`, `takeWholeSurvey`, `exportRespondents`),
so a caption like "mex2 resumes — and reads the wording their first visit was anchored to" is
written once and narrates every site it runs at.

Timings in the manifest are wall-clock. The assembler does not trust them as video positions — a
screencast drifts from wall time — it places each beat at the same *fraction* of its clip, against
the duration ffprobe reports.

### slowMo is not a luxury here

A recording run launches Chromium with a slowMo (180 ms; `-De2e.record.slowmo=`). Without it
Playwright presses the button in the same millisecond it moves the mouse there, and the cursor —
which is animated, because one that teleports reads as a cut rather than as a hand — is still in
flight when the page has already changed. The slowMo has to be **longer than the overlay's travel
time**, not merely non-zero. It also makes a Vaadin application legible: a form that fills instantly
shows a viewer nothing. Budget half an hour for a recorded run rather than fifteen minutes.

### Why the cards are rendered rather than drawn by ffmpeg

`drawtext` has no bidi and no Arabic shaping, so a card naming a site in its own language would come
out as disconnected letters in the wrong order. Chromium shapes them properly and is already
installed — the same reason `make-brand-images.py` renders the brand assets through it. Both go
through `browser_shot.py`.

Each site's badge wears the mark its own pages wear, which is what the three brands are for (see
"What makes a site look like itself"): Mexico's and Arabia's primaries are `#006847` and `#006C35`,
so color alone would not tell them apart, and the flag does.

### Checking the harness without the stacks

`RecordingHarnessSelfTest` exercises every piece — the injected overlay, the pointer's own count of
the clicks, the caption push, the per-visit clip, the manifest and `make-recording.py` itself —
against a page fulfilled by a Playwright route at each site's real base URL. No server, no stacks,
about fifteen seconds, and it leaves a real `journey.mp4` to look at:

```bash
mvn -DskipTests=false -De2e.record=true -Dtest=RecordingHarnessSelfTest test
```

Served rather than loaded from `file://` on purpose: the URL is what the overlay resolves the site
and application badge from, and a `file://` page would have proved nothing about it.

### When it does not work

| | |
|---|---|
| `nothing recorded yet` from `make-recording.py` | the run had no `-De2e.record=true`; `record.sh` always passes it |
| the film exists but has no pointer or caption | the overlay did not attach. Run the self-test below: if it passes, the harness is fine and the page is the problem |
| `ffmpeg is not on PATH` | `brew install ffmpeg`; the journey still records without it, only the assembly needs it |
| `no Chromium found` | Playwright's Chromium is not installed (see Prerequisites), or use `--no-cards`, which needs no browser |
| the clips are there but the film is short | a phase failed, so the journey stopped. `target/failure-*.png` says where, and the film ends there too |
| phase 1 fails with "already exists in Author" | the databases were not cleared; `./record.sh` without `--no-reset` does it |
| the pointer teleports instead of gliding | the slowMo was turned down below the pointer's travel time (`-De2e.record.slowmo=`, 180 ms default) |

### Known limits of a recording

- The lower third covers the bottom ~90px of the frame. Playwright centers what it scrolls to, so
  it rarely hides the element being clicked, but it can sit over a page's own footer.
- Nothing is narrated aloud, and the captions are English at every site — including while an Arabic
  page is being recorded. The pictures carry the language; the captions say what is being proved.
- A phase that fails stops the journey, so the film ends where the story broke. That is the same
  property the suite has, and it is usually what you want to look at.

## Layout

- `usa/`, `mexico/`, `arabia/docker-compose.yml` — self-contained, trimmed copies of the umbrella
  compose file (see the comments at the top of each). Data lives under `data/<site>/` (ignored).
- `mexico/mexico_brand/`, `arabia/arabia_brand/` — each site's brand mount, beside the compose
  file that mounts it. Source artwork is `images/flag.svg`; the PNGs and the `.ico` beside it are
  generated by `./make-brand-images.py`. USA mounts `../elicit-brand` unchanged.
- `census-household-survey.elicit` — the definition phase 1 imports: the sample of the same name
  with one deliberately broken rule (see "The definition the master imports").
- `pom.xml` — borrows the page objects of `../e2e-tests` by source (build-helper) and runs only
  `**/multilingual/*E2ETest.java`.
- `src/test/java/com/elicitsoftware/e2e/multilingual/` — `Site`, `MultilingualTestBase`,
  `CensusHouseholdSurvey` (where every question sits and how it is answered), `ContentTranslations`
  (the translator's side of the hand-off) and the journey.
- `src/test/resources/fixtures/` — the translations and the two tools that make and check them.
- `src/test/resources/recording/overlay.js` — the pointer and the caption, injected into every
  recorded page; `Recording.java` is the Java half and `RecordingHarnessSelfTest` checks both.
- `record.sh`, `make-recording.py` — record a run, and assemble it into `journey.mp4`.
- `browser_shot.py` — finding Playwright's Chromium and screenshotting HTML with it, shared by
  `make-brand-images.py` (brand assets) and `make-recording.py` (title cards).

## Defects this suite needed fixed first

The census household survey could not be walked end to end before this suite existed;
`../samples/FINDINGS.md` records why. Four of those findings were fixed to make the journey
possible, and they are fixes to the platform, not to the test — three of them hit any survey with
the shape in question, not only this sample:

- **Survey `SectionView`** saved a COMBOBOX answer as `e.getValue().toString()`, which persisted
  `SelectItem`'s identity hash — meaningless in `surveyreport`, unmatchable by any rule, and
  unreadable by `ElicitComboBox.setValue` on the next visit, so a combobox answer never survived
  the respondent leaving and coming back. It now saves `codedValue`, as RADIO always did
  (FINDINGS 1).
- **Survey `QuestionManager.sqlStep`** lacked the question-only `UNION` branch that `sqlSection`
  has, so a rule whose target sits in the upstream question's own section did not hide its target
  when a step was built. The other-race and housing-subsidy questions were visible from the first
  render (FINDINGS 2).
- **Survey `SectionView.buildQuestions`** tested for a section title with
  `answer.question == null && answer.sectionInstance == 0`. A section title is exactly the row that
  carries no question; a REPEATed section's instances are numbered 1, 2, … and have a title row
  each, so they fell through to the question branch and threw an NPE, which `nextSection()` caught
  and showed as a navigation error (FINDINGS 5a).
- **Survey `DisplayKey.getSectionString()`** zeroes the section instance, and
  `QuestionManager.getCurrentNavItem` matched navigation items with it. Navigation items are built
  from the respondent's own section-title answer rows, whose keys carry the real instance, so a
  repeated section never found its own item: no Previous/Next buttons and no title. The
  structure-naming form is unchanged and a `getSectionInstanceString()` was added for the
  navigation lookup (FINDINGS 5b).
- **The sample's own rules** chained steps 3, 4 and 6 on `FIELD_EXIST`, which returns `true`
  unconditionally, so the whole survey was built at once the moment its upstream step materialised.
  Those three rules now test the consent checkbox with `BOOLEAN`, an operator that actually reads a
  value; `FIELD_EXIST` is still used for the one thing it does express — one step instance per
  REPEATed answer row (FINDINGS 3).

FINDINGS 4 (`questions.mask` is inert) and 6 (the English-only token fix-ups, and token values
filling from only some question types) are untouched: neither blocks this journey. The English-only
fix-ups in `QuestionManager.replaceTokens` are worth knowing about here, though — they run whatever
the respondent's language is, so a Spanish or Arabic text whose token substitution lands next to an
English possessive rule could come out subtly wrong. This survey's tokens do not hit that case.

One more defect is **not** fixed and does not block anything: Survey's shipped
`META-INF/i18n/i18n-config.json` has had a license header prepended to it by the build, so it is not
valid JSON. Every start where the translations mount has no `i18n-config.json` of its own logs
`Malformed i18n-config.json; using built-in direction defaults` and the shipped direction overrides
are ignored. Arabic is in the built-in right-to-left list, so this journey is unaffected; a language
that is not would be.

## Known limits

- Author runs on the master only; the remote sites import definitions and export respondents.
- The consoles are driven in English everywhere. The suite asserts that each remote console *offers*
  its language, but does not drive Admin through a translated console: the Admin page objects locate
  buttons and labels by their English captions. Driving a console in Spanish would need a
  language-aware set of them, as `SectionPage` now has for the two navigation captions.
- Respondent languages are chosen with the `?lang=` link parameter and the browser's
  `Accept-Language` is pinned to `en-US`, so the link is always what decides (UC-009 step 2). The
  selector itself is asserted to be offered but is not clicked; the session-memory path (step 5) is
  not covered.
- The reporting star (`surveyreport`) is only populated for the survey the site installed; the
  imported respondents are complete in `survey.*` but do not appear in the master's reporting.
- `surveyreport.dim_step.value` and `dim_section.value` are unique per site, so two different
  surveys sharing a step or section dimension name make the Survey ETL fail. Revisions of one
  survey are fine, since dimension rows are keyed by the durable step/section id.
- All three sites must share the same `.elicit` lineage: element keys and versions are what the
  respondent import resolves.
- The three Survey containers run with `elicit.content.translations-ttl: PT5S`. A site holds a
  survey's translations for five minutes by default before reloading them, so an Admin apply
  "surfaces within this window without a restart" — which also means a respondent who logs in
  seconds after an apply reads the **base language** for every string whose translation changed in
  it. The journey applies a revision and sends a respondent in immediately, so it turns the window
  down rather than sleeping through it. This is deployment configuration, not a workaround for a
  defect, but it is worth knowing that a production site behaves the other way: after a revision
  that rewords a translated question, respondents read that question in the base language for up to
  five minutes.
- Greenfield databases only; the V2 brownfield path of `../resetDatabase.sh` is not exercised.
- Nothing asserts the reports (Survey UC-005) or the emails, in any language.
