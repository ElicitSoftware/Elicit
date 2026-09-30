# The installation manual (umbrella UC-001)

`Installing the Platform` — the printable manual that takes a deployment operator
from an empty machine to a running, verified Elicit site, and is the single
reference for every setting that site is configured with. It covers Survey and
Admin.

See [`../use_cases/UC-001-consult-the-installation-manual.md`](../use_cases/UC-001-consult-the-installation-manual.md)
and [`../use_cases/UC-002-build-the-installation-manual.md`](../use_cases/UC-002-build-the-installation-manual.md)
for the behavior this implements, and FR-001/FR-002 with NFR-001–NFR-010 in
[`../requirements.md`](../requirements.md). Its chapters map one to one onto
umbrella use cases UC-003 to UC-019.

## This manual ships with no image

The administrator's manual is packaged into the Admin image and served through a
role check. This one is **not** (umbrella C-001). An operator reads it *before*
there is an Elicit service to serve it from, so it is a release artifact of this
repository and nothing more. Publish the PDF with the release.

## What is here

| Path | Committed? | What it is |
| --- | --- | --- |
| `elicit-installation-manual.tex` | yes | the manual |
| `elicit-brand.sty` | yes | the Elicit default brand as LaTeX: the shared manual design plus the settings tables, the copy-safe verbatim environments and `\elicitProduct` |
| `images/` | yes | the 18 captured figures, plus `elicit-logo.png` for the title page |
| `capture/` | yes | `capture-screenshots.mjs` and its own `package.json`; regenerates `images/` from a freshly installed stack |
| `build-manual.sh` | yes | typesets the PDF |
| `check-properties.sh` | yes | checks the configuration reference against the code (NFR-004, NFR-005) |
| `build-info.tex` | **no** | generated: the version and build date stamped into this build |
| `build/` | **no** | generated: LaTeX's working directory |
| `elicit-installation-manual.pdf` | **no** | generated: the manual |

## Building it

```bash
docs/manual/build-manual.sh                       # version from the modules' pom, date is now
docs/manual/build-manual.sh 3.0.0 "2026-09-24"    # explicit stamp
./buildDockerImages.sh Manual                     # check-properties.sh, then the same build
```

`buildDockerImages.sh` builds the manual alongside the module images by default,
so a full build stamps the PDF from the same tree the images come from. That
target runs `check-properties.sh` first and typesets nothing if it fails;
`SKIP_PROPERTY_CHECK=1` builds without the check.


There is no TeX installation to maintain: the script typesets inside a TeX Live
container (`TEXLIVE_IMAGE`, default `texlive/texlive:latest`), which is where IBM
Plex and the LaTeX packages come from. Docker must be running. `SKIP_MANUAL=1`
skips the build and exits zero.

The umbrella holds no `pom.xml` of its own, and its git tags version the
*repository*, not the platform — `v1.1.1` while the modules are at `3.0.0`. So an
unstamped build takes the version Survey and Admin carry, and uses it only when
both agree. If they disagree, or a module is not cloned, it stamps `unknown`
and says why: a wrong version on the title page is worse than an absent one.
`unknown` is *written*, never omitted (UC-002 BR-003), so a printed copy is never
silently undated.

## Keeping the configuration reference honest

```bash
docs/manual/check-properties.sh
```

Run it before a release, and after any change to a module's
`application.properties` or its `@ConfigProperty` declarations. It reports:

1. every key a module reads through `@ConfigProperty` or a programmatic
   `getConfig()` lookup that the manual does not document (NFR-004);
2. every Elicit key the manual documents that nothing reads any more;
3. every documented default that disagrees with the module's own sources
   (NFR-005).

It exits non-zero on any of the three, so it can gate a release. It resolves
`${VAR:default}` expressions to their defaults, because that is what the manual
prints. Keys that the manual deliberately covers as a group — the ~150
OpenTelemetry, Micrometer, container-image and per-category logging settings —
are listed as prefixes in `IGNORED_PREFIXES` inside the script; add to that list
rather than documenting a key one at a time when it genuinely belongs to a group.

The script needs `Survey/` and `Admin/` cloned, and says so if they are not.

## Regenerating the screenshots

The 18 figures are committed. Regenerate them only when the screens they show
change — and read this section first, because **three of them cannot be retaken
without destroying the local database.**

```bash
cd …/Elicit
./resetDatabase.sh V3          # deletes postgresql/PGDATA — greenfield
docker compose up -d           # one pass; no restart sequence
npm --prefix docs/manual/capture install
npm --prefix docs/manual/capture run capture
```

Figures `05`, `06` and `07` are **first-run screens**: the blocking
no-department dialog with both setup banners, the empty Departments list and the
first department form. They exist only on a database that has never had a
department, so the script takes them before it creates one. Figures `04` (the
sign-in page) and `08` (the console once a department exists) are reproducible on
any stack and are not part of that set.

On a stack that already has a department the run **skips** those three, says so
with the reason, and leaves the committed figures untouched — it does not
photograph an ordinary console and pass it off as the first run. So a capture
without the reset is safe and still refreshes the other fifteen; reset only when
one of the three first-run screens is what changed.

The script then does the install itself, in order: it creates the department
(which is what dismisses the modal dialog — until it does, every later click is
intercepted and nothing else is capturable), applies
`FHHS/family-history-survey.elicit`, and walks the System pages, the branded
Survey header and the language selector.

A step that cannot find its screen warns and carries on, so one changed selector
costs one figure rather than the whole run; the exit status is non-zero if any
figure was missed, and `FAILED.png` is written where an unhandled error stopped
it. Delete that file before committing.

Env: `ADMIN_URL`, `SURVEY_URL`, `KEYCLOAK_URL`, `ADMIN_USER`, `ADMIN_PASSWORD`,
`KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD`, `MANUAL_IMAGES`, `MANUAL_DEFINITION`.

After regenerating, check that every `\screenshot{…}` caption in the `.tex` still
describes what the figure shows — the caption is part of the instruction.

### Figure 18 is captured with the selector open

The selector's rows are labeled from the JDK's own names for each language, so
they are readable whatever the bundles translate. They render *after* the
overlay opens, though: the script waits 2.5 s before the shot, because a shorter
wait once photographed them blank and that blankness was mistaken for a defect.
The step fails rather than skips when the selector is absent — a Survey narrowed
to `en` hides it, and a closed selector would only repeat figure 17.

## When the manual and a module disagree

The manual is stamped with one platform version. Release-specific upgrade
procedures stay in `DeploymentScript.md` (umbrella C-006) — a stamped manual
outlives the release it was built for, so it points at them rather than
restating them.
