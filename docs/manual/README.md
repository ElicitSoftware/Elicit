# The installation manual (umbrella UC-001)

`Installing the Platform` — the printable manual that takes a deployment operator
from an empty machine to a running, verified Elicit site, and is the single
reference for every setting that site is configured with. It covers Survey,
Admin and Author.

See [`../use_cases/UC-001-consult-the-installation-manual.md`](../use_cases/UC-001-consult-the-installation-manual.md)
and [`../use_cases/UC-002-build-the-installation-manual.md`](../use_cases/UC-002-build-the-installation-manual.md)
for the behaviour this implements, and FR-001/FR-002 with NFR-001–NFR-010 in
[`../requirements.md`](../requirements.md). Its chapters map one to one onto
umbrella use cases UC-003 to UC-019.

## Unlike the author's manual, this one ships with no image

The author's manual is packaged into the Author image and served through a role
check. This one is **not** (umbrella C-001). An operator reads it *before* there
is an Elicit service to serve it from, so it is a release artifact of this
repository and nothing more. Publish the PDF with the release.

## What is here

| Path | Committed? | What it is |
| --- | --- | --- |
| `elicit-installation-manual.tex` | yes | the manual |
| `elicit-brand.sty` | yes | the Elicit default brand as LaTeX. The same design as `Author/docs/manual/elicit-brand.sty` plus the settings tables, the copy-safe verbatim environments and `\elicitProduct` |
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
unstamped build takes the version the three modules carry, and uses it only when
all three agree. If they disagree, or a module is not cloned, it stamps `unknown`
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

The script needs `Survey/`, `Admin/` and `Author/` cloned, and says so if they
are not.

## Regenerating the screenshots

The 18 figures are committed. Regenerate them only when the screens they show
change — and read this section first, because **six of them cannot be retaken
without destroying the local database.**

```bash
cd …/Elicit
./resetDatabase.sh V3          # deletes postgresql/PGDATA — greenfield
docker compose up -d           # one pass; no restart sequence
npm --prefix docs/manual/capture install
npm --prefix docs/manual/capture run capture
```

Figures `04` to `08` are **first-run screens**: the sign-in, the blocking
no-department dialog with both setup banners, the empty Departments list, the
first department form, and the console once the department exists. They only
exist on a database that has never had a department or a survey, so the script
captures them before it creates the department and before it applies the
definition. Running the capture against an installed stack silently produces
the wrong pictures for those five — reset first.

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

### Figure 18 is captured with the selector closed, on purpose

The language selector's dropdown currently renders the mounted Spanish and
Arabic rows with **blank labels** (English alone is labelled) in both Survey and
Admin. The figure shows the closed selector in the header, because what it is
there to demonstrate is that the selector appears at all — which happens only
once a second language is on the mount. Open it again for the figure when that
defect is fixed.

## When the manual and a module disagree

The manual is stamped with one platform version. Release-specific upgrade
procedures stay in `DeploymentScript.md` (umbrella C-006) — a stamped manual
outlives the release it was built for, so it points at them rather than
restating them.
