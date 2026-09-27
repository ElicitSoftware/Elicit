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
2. **The site must have the language for its own screens.** The applications ship English only;
   every other language is a `translations_<tag>.properties` file an operator drops into the
   translations mount (`i18n.file.system.path`, `/opt/i18n` in Docker). A site serves a survey
   language only where that language is *also* mounted for the chrome (Survey UC-009 BR-009), so no
   respondent ever reads translated questions between English buttons.

`./sync-i18n.sh` lays out the three mounts from `../elicit-i18n`, the platform's own translation
source, and `./up.sh` runs it first. That is the "i18n files" half of distribution: the definition
file carries the survey's content translations, the properties files carry the applications' own
texts, and they arrive by different routes.

The master mounts **empty** application directories on purpose. It therefore hides the language
selector (an English-only deployment) and serves the survey's questions in English even though the
Spanish and Arabic translations are sitting in its own database — which is exactly BR-009, and the
journey asserts it.

## The story the test runs

`CensusMultilingualE2ETest` is one ordered story, one JUnit method per phase; a failed phase marks
the rest skipped so the report shows where the theory broke. Every persona visit (the author, each
site's administrator, every respondent login) runs in a fresh browser context — which matters more
here than in `../e2e_multisite`, because the chosen language lives in the browser session.

1. USA's author **imports** `../samples/census-household-survey.elicit` into Author (UC-005) and
   checks it: the phase fails with the validation panel's own text unless the definition is clean
   (UC-007). That survey exercises every question type and every rule the engine supports.
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
bundles from `../elicit-i18n`, which is also where the two navigation captions a respondent's
`SectionPage` needs come from.

## Prerequisites

- Docker Desktop, and the `survey`, `admin` and `author` images built from the umbrella root:
  `./buildDockerImages.sh Survey Admin Author`. Pedigree and FHHS are not needed.
- The umbrella checkout, for `../elicit-i18n`, `../elicit-brand`, `../keycloak`, `../db-init` and
  `../samples/census-household-survey.elicit`.
- Playwright's Chromium, installed once:
  `mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"`
  (from `../e2e-tests`, or here after a first `mvn test-compile`).
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

## Layout

- `usa/`, `mexico/`, `arabia/docker-compose.yml` — self-contained, trimmed copies of the umbrella
  compose file (see the comments at the top of each). Data lives under `data/<site>/` (ignored).
- `i18n/<site>/` — the translations mounts, laid out by `./sync-i18n.sh` from `../elicit-i18n`
  (the copies are ignored; the directories are kept by `.gitkeep`).
- `pom.xml` — borrows the page objects of `../e2e-tests` by source (build-helper) and runs only
  `**/multilingual/*E2ETest.java`.
- `src/test/java/com/elicitsoftware/e2e/multilingual/` — `Site`, `MultilingualTestBase`,
  `CensusHouseholdSurvey` (where every question sits and how it is answered), `ContentTranslations`
  (the translator's side of the hand-off) and the journey.
- `src/test/resources/fixtures/` — the translations and the two tools that make and check them.

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
