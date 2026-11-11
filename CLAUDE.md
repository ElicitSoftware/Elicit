# Elicit — Umbrella Repo Context

This repository is the **meta project** for the Elicit platform. It holds no
application source of its own: it is the place the individual module repos are
cloned into, plus the compose file, brand assets, and scripts that wire them
together into a runnable local system.

Elicit is a modular survey platform built by Michigan Medicine — authoring,
delivery, administration, and custom reporting. See `README.md` for the
public-facing description of each module.

## Repo Topology — The Modules Are Not Submodules

There is **no `.gitmodules`**. Each module is an independent clone of its own
GitHub repo, dropped into this directory and excluded from this repo via
`.gitignore`. `git status` here will never show module changes.

| Directory     | Repo                         | What it is                                     |
| ------------- | ---------------------------- | ---------------------------------------------- |
| `Survey/`     | `ElicitSoftware/Survey`      | Subject-facing survey app (Quarkus + Vaadin)    |
| `Author/`     | `ElicitSoftware/Author`      | Authoring tool; exports `.elicit` definitions   |
| `Admin/`      | `ElicitSoftware/Admin`       | Administrator app (Quarkus + Vaadin)            |
| `FHHS/`       | `ElicitSoftware/FHHS`        | Family Health History Survey; headless REST     |
| `Pedigree/`   | `ElicitSoftware/Pedigree`    | R/Kinship2 pedigree-drawing service (plumber)   |
| `Prometheus/` | *tracked here*               | Prometheus config; not a repo, not in compose   |
| `postgresql/` | *not tracked, not a repo*    | Local `PGDATA` volume only                      |

`cloneAllProjects.sh` bootstraps all five module clones.

Author was a private repository from 2026-09-29 until Elicit#72 released it with the
other modules. Everything it shares with the platform lives here once: the stack
(`docker-compose.yml`), the Keycloak realm (with the `elicit-author` client), the
`author` database script (`db-init/`), the curated translations
(`elicit-i18n/author/`) and the three end-to-end suites. Author's integration branch is
`main`, like Pedigree's; it has no `V3`.

### Working Across Modules

- Edits inside a module directory belong to **that module's repo** — commit,
  branch, and open PRs there, not here.
- Modules routinely sit on **different branches** at the same time. Check
  `git -C <module> rev-parse --abbrev-ref HEAD` before starting; never assume
  they are all on `main`.
- A cross-cutting change (a schema change touching Survey + Admin + FHHS, say)
  needs a branch and a commit per repo. Confirm the branch/PR strategy with
  the user before committing across repos.
- The four Java modules (Survey, Admin, FHHS, Author) each carry their own
  `CLAUDE.md` and `AGENTS.md` with the conventions that govern them. Those take precedence inside their
  subtree. `Pedigree/` and `Prometheus/` have no such file.

## Terminology: Access Code vs. Token

The credential a respondent enters to reach a survey is the **access code**
(`survey.respondents.access_code`, `accessCode` in Java, `<ACCESS_CODE>` in email
templates, `{AccessCode}` in the FHHS SFTP XML template). "Token" means only a
question-text placeholder (`{<KEY>|default}`, `survey.relationships.token`) or an
OIDC/Bearer token. Never call the respondent credential a token.

## Module Conventions (shared by Survey, Admin, FHHS)

- Java 25, Quarkus 3.40.1.1 LTS, Maven. Vaadin 25.3.4 Flow for the two UI apps;
  FHHS is headless REST.
- **Hibernate ORM with Panache — not jOOQ.** Flyway migrations live under
  `src/main/resources/db/migration`.
- All three follow the **AI Unified Process** (AIUP): `docs/` is the source of
  truth for behavior, `src/` is the implementation. The `aiup-core` plugin
  supplies the skills (`/requirements`, `/entity-model`, `/use-case-diagram`,
  `/use-case-spec`, `/reverse-engineer`).
- They share one PostgreSQL database and the `survey` schema, which the Survey
  app owns.
- Use the **Quarkus Agent MCP tools** rather than raw `mvn` — these are
  existing projects, so start with `quarkus_update` and `quarkus_skills`. Do
  not run `mvn clean` while dev mode is running.

## Local Stack (`docker-compose.yml`)

Applications: `survey`, `admin`, `fhhs`, `pedigree`, `author` and `author-survey` (a
second Survey on the `author` database, Author's respondent preview). Supporting services:
`db` (PostgreSQL), `keycloak` (OIDC), `mailpit` (SMTP), `sftpServer`, and
`jaeger` (OpenTelemetry). All app images are built locally as
`elicitsoftware/<name>:latest`, and the compose file pulls every application image
at one tag, `${ELICIT_TAG:-latest}`, so `ELICIT_TAG=3.0.0-beta.1 docker compose up -d` (or a
`.env` line, ignored by git) runs a published release as a matched set. `elicit_db` is not on
the tag.

| Port    | Service                                    |
| ------- | ------------------------------------------ |
| `8080`  | Survey                                     |
| `8081`  | Admin                                      |
| `8082`  | FHHS                                       |
| `8083`  | Pedigree                                   |
| `8084`  | Author                                     |
| `8085`  | Author preview (`author-survey`)           |
| `8180`  | Keycloak (admin/admin)                     |
| `8025`  | Mailpit web UI                             |
| `16686` | Jaeger UI                                  |
| `5452`  | PostgreSQL (host) → `5432` in container    |

Healthchecks target the **container-internal** port `8080`, not the published
host port. Every service exports OTLP traces/metrics/logs to `jaeger:4317`.

Author works in a database of its own, `author`, in the same cluster; `db` creates it
from `db-init/V0.0.4__CREATE_AUTHOR_DATABASE.sql` on first initialization only, so an
existing `PGDATA` needs that script run once by hand (see its header). For Author in
dev mode on 8084, start what it calls: `docker compose up -d db keycloak author-survey`.
Authoring needs none of the site applications.

`PREMM5` is commented out in the compose file and is not cloned by
`cloneAllProjects.sh`.

### Prometheus Is Not Wired Into This Compose File

`Prometheus/` (config, recording rules, alerting rules) and `docs/metrics/*`
describe a metrics pillar that **does not run here**. There is no `prometheus`
service in `docker-compose.yml`, so nothing scrapes the `/q/metrics` endpoints
and nothing serves `localhost:9090`, despite what `Prometheus/README.md` and
the observability guide say.

Prometheus and Jaeger are also **independent of each other** — the PromQL
queries are not consumed by Jaeger. Jaeger here is `all-in-one:1.53` with only
`COLLECTOR_OTLP_ENABLED`, i.e. traces only. Its Service Performance Monitoring
feature *can* read RED metrics from Prometheus, but that needs
`METRICS_STORAGE_TYPE=prometheus` and `PROMETHEUS_SERVER_URL`, and neither is
set. The Monitor tab in the Jaeger UI is therefore inert.

The scrape config also still targets `premm5:8080` and assumes
`postgres-exporter` and `cadvisor`, none of which exist in this compose file.

### First Run: Nothing Is Seeded, the Import Provides It

`docker compose up -d` creates the schema in one pass: Survey creates it, Admin adds its
tables, and FHHS runs its own migrations (grants, indexes on the star schema, sequence
hygiene). **No survey and no department are seeded.** The two accounts `admin` and
`user` are, so the console can be signed into.

The first sign-in as `admin` is blocked by a modal dialog until a department exists
(Admin UC-028): follow its link to Departments and create one, which is assigned to the
creator. A `user` with no department can only log out. Existing databases keep their
seeded "Testing Department"; the dialog is only seen on a database created after this
change.

The Family History Survey arrives by import: Admin > Apply Survey Definition with
`FHHS/family-history-survey.elicit`. The apply asks Survey to build that survey's reporting
star schema (`POST /api/etl/build?survey=<key>`), so **no Survey restart is needed**. Every
survey gets a schema of its own (Survey UC-008): `surveyreport` holds only the shared
`dim_date` and `dim_status`, and the survey's star — `dim_step`, `dim_section`,
`dim_question`, `dim_item`, one `dim_<tag>` per reporting tag, `fact_sections`, the
`fact_respondents` view and the two `*_view`s — is created by the apply in a schema named
`report_<slug of the survey name>` (`report_family_history_survey` for FHHS) and recorded on
`survey.surveys.report_schema`. Confirm with:

```sh
docker exec elicit-db-1 psql -U survey -d survey \
  -c "select name, report_schema from survey.surveys;" \
  -c "select count(*) from report_family_history_survey.dim_step;"
```

`report_schema` must be set and the count non-zero (15 for the Family History Survey). The
schema name is site-local (not in the `.elicit`) and can be changed afterwards with
`POST /api/etl/schema/<key>/rename?name=<new>` on Survey; nothing in Elicit hard-codes it.

FHHS is specific to that survey (FHHS UC-005). Until it is imported **and built** (its
`report_schema` set by the apply), FHHS starts, logs one WARN naming the survey key and what
to do — the import, or the build if the survey is there but has no schema yet — reports
**not-ready** on `/q/health/ready`, and answers report requests with 503 carrying the same
message. It resolves the schema name from `survey.surveys.report_schema` on every request and
goes healthy by itself on the next probe after the apply; nothing needs restarting. FHHS
starts after Admin is healthy (not the other way round): on a fresh database the two
modules' first migrations grant on the same tables and collide with "tuple concurrently
updated" when they run together, and Admin needs nothing from FHHS to come up, so the
import is always reachable. `deploy.sh` (`up -d`, then restart Survey) is only
needed on a stack whose survey was seeded before this change.

The fact tables stay empty until respondents exist; they fill through the
`fact_respondent_insert` and `fact_update` triggers on `survey.respondents`.

`resetDatabase.sh V3` stops the stack and deletes `postgresql/PGDATA` for a
greenfield run. `resetDatabase.sh V2` replaces it with a copy of `PGDATA_v2`, the
pre-Kimball V2 database, for a brownfield (upgrade) run. `PGDATA_v2` is tracked in
git, which drops the empty directories PostgreSQL needs (`pg_notify`, `pg_tblspc`
and eleven more), so the V2 mode recreates them in the copy; never copy it by hand.
`PGDATA_v2` also predates Author's `author` database, so the V2 mode runs
`db-init/V0.0.4__CREATE_AUTHOR_DATABASE.sql` while the database is up for the collation
repair; without it `author-survey` never becomes healthy and Author does not start.

If the db container's first start after a reset fails with `data directory
"/var/lib/postgresql/data/pgdata" has wrong ownership`, that is a Docker Desktop
for Mac VirtioFS glitch (docker/for-mac#7415), not a broken copy: bind mounts show
every file as root-owned inside the container and PostgreSQL's owner check can
fail on initial startup. Run `resetDatabase.sh` again and restart; if it recurs,
switch Docker Desktop's file sharing to gRPC FUSE. Both reset paths were verified
to start cleanly on 2026-09-20.

## Scripts

- `cloneAllProjects.sh` — clone the five module repos.
- `checkLanguages.sh` — the Survey, Admin and Author images carry English only,
  `elicit-i18n/` gives all three the same languages, and Author's
  `author.content.languages` is exactly that set. Run by `buildDockerImages.sh` before it
  builds anything.
- `buildDockerImages.sh [Target ...]` — build the module images in parallel
  (all five by default, or just the named ones), one log per target under
  `build-logs/`, with a summary and a non-zero exit if any build failed. The
  sixth default target, `Manual`, is not a module and builds no image: it runs
  `docs/manual/check-properties.sh` and then `docs/manual/build-manual.sh`, so
  the installation manual is gated against the code and stamped from the same
  tree as the images (`SKIP_MANUAL=1`, or naming targets without it, skips the
  target; `SKIP_PROPERTY_CHECK=1` typesets without the gate; a `Manual`-only run
  prints no port warnings). There
  is no build-time dependency between the images; each module's tests use a
  distinct Quarkus test port (Survey 8089, Admin 8090, FHHS 8091, Author 8092).
  PREMM5 is not built (that module is not cloned and is commented out of
  compose), and there is no `postgresql/` build step — `elicitsoftware/elicit_db`
  is pulled, not built. Ctrl-C stops the module builds too (Bash would otherwise leave them
  running as orphans that hold `target/` and the test ports while the lock is released),
  and the script warns about listeners on ports 8080-8084 and 8089-8092 before it starts:
  a dev-mode Survey on 8080 stalls the Survey test suite, because the test data points
  post-survey-action and report URLs at `localhost:8080` and dev mode parks requests while
  it restarts, so each such call waits out the Survey clients' 60-second answer timeout
  before it fails.
- `buildManuals.sh [Target ...]` — typeset every manual fresh, one after another: `Platform`
  (the installation manual here, gated by `check-properties.sh`), `Admin`, `Author` (the
  in-application manuals) and `AuthorInstallation` (gated by the same script). Each runs its own
  `build-manual.sh` after removing that manual's latexmk `build/` directory. A module that is not
  cloned is skipped; a failure does not stop the rest, and the run exits non-zero. The module image
  scripts reuse the PDFs it writes, so `./buildManuals.sh && ./buildDockerImages.sh` gives images
  carrying manuals stamped by this run.
- `deploy.sh` — bring up an initialized stack.
- `resetDatabase.sh V2|V3` — reset `postgresql/PGDATA` to the V2 copy (brownfield)
  or delete it (greenfield); stops the stack first.
- `status.sh` — container status plus a probe of each service's health endpoint:
  `/q/health/ready` for Survey, Admin, FHHS, Author and its preview (printing why one is
  not ready), `/healthz` for Pedigree, the front page for Keycloak and Mailpit.
- `generateGithubTemplates.py` — regenerate the pull request template and issue forms of
  all six repositories from one table; commit the result in each.

## End-to-End Suites

`e2e-tests/`, `e2e_multisite/` and `e2e_multisite_multilingual/` are standalone Maven
projects (no parent pom) holding the platform's Playwright tests. They drive Author, Admin
and Survey together, and the two multi-site suites compile the page objects out of
`e2e-tests/src/test/java` by relative path, so the three stay siblings.

- They run through `mvn`, not the Quarkus Dev MCP tools: each suite's `run.sh` calls
  `mvn -DskipTests=false test` against a stack that is already running. `skipTests`
  defaults to true.
- They need the images first (`./buildDockerImages.sh`, Author included).
- `e2e-tests/` runs against this directory's `docker-compose.yml`. The multi-site suites
  bring up stacks of their own (`up.sh`/`down.sh`) on the same host ports, so only one of
  the three stacks runs at a time.

## Branding

`elicit-brand/` is the default brand (colors, typography, `theme.css`,
`brand-config.json`), mounted read-only at `/opt/brand` in the Survey and
Admin containers via `brand.file.system.path`. `test-brand/` and
`test-partial-brand/` exercise fallback behavior when a brand omits assets.
See `docs/BRAND_SYSTEM_IMPLEMENTATION_GUIDE.md`.

## Translations (i18n)

**The images carry English only.** The English bundle is authored at
`src/main/resources/vaadin-i18n/translations.properties`, Vaadin's standard location. Every other
language is a `translations_<tag>.properties` file the deployment mounts at
`<i18n.file.system.path>/<i18n.app.name>/` (`/opt/i18n/survey/` and `/opt/i18n/admin/` here). A
mounted file overlays English key by key, so it may be partial, and a missing key falls back to
English. Nothing mounted means English only and no language selector.

`elicit-i18n/` in this repo is the **curated set** ElicitSoftware maintains beside each release:
`survey/`, `admin/` and `author/`, each with Latin American Spanish (`es_419`) and Arabic (`ar`),
plus `i18n-config.json`. `docker-compose.yml` mounts it read-only into Survey, Admin, Author and
the preview Survey. The module tests read it from
`../elicit-i18n` (`%dev` and `%test` profiles), and their CI fetches it from the umbrella branch
with the **same name** as the module branch. A change to translations or to English keys therefore
needs a branch of the same name here.

What a site controls:

- **What it mounts:** the curated set, a subset of it, or files of its own.
- **`i18n.offered.locales`:** offers fewer languages than are mounted. A language left out is
  unreachable: hidden from the switcher, refused by `?lang=`, and not served for survey content
  either. English is always offered, and a listed tag with no mounted file is not offered.
- **`i18n.bundled.locales` is retired.**

`checkLanguages.sh` runs before `buildDockerImages.sh` builds anything. It fails when:

- a translated bundle has crept back into an image;
- a module still sets `i18n.bundled.locales`;
- `elicit-i18n/survey`, `elicit-i18n/admin` and `elicit-i18n/author` hold different languages. A
  language one app has and another lacks cannot be served.

The authoring tool reads `elicit-i18n/author/` from the same mount (Author#79) and is the third party
to that invariant: it also declares which languages a survey's *content* may be published in
(`author.content.languages`). The check holds Author to the same rules as Survey and Admin, and a
content language set that differs from the mount fails the build too.

Direction (RTL/LTR) follows the language. It and the font scale are layered, each layer winning
over the one before:

1. `META-INF/i18n/i18n-config.json` on each app's classpath (`ar` is `rtl` at `fontScale` 1.15);
2. `i18n-config.json` at the mount root;
3. `i18n.direction.<tag>` or `i18n.font-scale.<tag>` for one tag.

 The scale multiplies the root font
size through `--elicit-font-scale` and one `html { font-size: … }` rule in each app's `styles.css`,
so it grows the whole page for that language. Brand names are translated in the brand's own
`localized` block, never in the app bundles. Survey content in the database is translated by a
separate mechanism (`survey.translations`, Survey V019): authored in Author, carried in the
`.elicit` file, and served only where the survey publishes the language *and* the site offers it.
See `docs/I18N_IMPLEMENTATION_GUIDE.md`.

## Umbrella Docs

The umbrella now carries its own AIUP artifacts — the first ones it has had —
scoped to the platform as one deployable whole: `docs/vision.md`,
`docs/requirements.md` (FR-001..037, NFR-001..012, C-001..014),
`docs/use_cases.puml` and `docs/use_cases/UC-001..019`. Module requirement and
use-case IDs are unrelated to these.

- `docs/manual/` — the **installation manual** (UC-001), a LaTeX/PDF that covers
  Survey and Admin: topology, prerequisites, the database roles and
  schemas, the OIDC clients and roles, both installation paths, the first
  sign-in, the configuration reference, branding, translations and verification.
  Built by `docs/manual/build-manual.sh` through a TeX Live container, stamped
  with the version the two modules agree on, and built alongside the images by
  `buildDockerImages.sh` (target `Manual`). Unlike the in-application manuals it is
  **not** packaged into any image — an operator reads it before any Elicit
  service exists. `docs/manual/check-properties.sh` gates the configuration
  reference against the modules' `@ConfigProperty` declarations and
  `application.properties`; `buildDockerImages.sh` runs it before typesetting, so
  a drifted reference fails the build rather than reaching a release. Author's
  installation manual is separate (in Author, not built here), but its build runs
  this same script with `--manual … --modules Author`, and Author's two manuals
  read `elicit-brand.sty` from here: keep both working for Author.
- `DeploymentScript.md` — non-Docker deployment, plus the procedure for
  upgrading an existing deployment to Kimball Type 2 SCD (V3.0.0). Upgrade
  procedures stay here and are deliberately **not** in the installation manual,
  which is stamped with one version and outlives it.
- `docs/BRAND_SYSTEM_IMPLEMENTATION_GUIDE.md`
- `docs/I18N_IMPLEMENTATION_GUIDE.md`
- `docs/metrics/OBSERVABILITY_IMPLEMENTATION_GUIDE.md`,
  `docs/metrics/PROMETHEUS_QUERIES.md`

## Git Workflow

- `main` and `V3` (and `main` in Pedigree and Author) change only through a pull request
  whose CI check passed. A ruleset on each repository enforces it on the server (umbrella
  C-016), and `.claude/hooks/branch-guard.py`, wired as a `PreToolUse` hook in each repo's
  `.claude/settings.json`, refuses `git commit`, `git cherry-pick`, `git revert` and any
  `git push` that would land on `main`, `master`, `V2` or `V3`. It resolves `git -C <dir>`
  and `cd <dir> &&` chains, so module repos are guarded from the umbrella too.
- Every change starts from an issue in the repo that owns the code; a cross-cutting change
  gets a tracking issue here with one PR per module repo that references it. Branch from the
  integration branch (`V3`, or `main` for Pedigree and Author) as `<type>/<issue>-<slug>`,
  with `<type>` one of `feature`, `fix`, `docs`, `chore`.
- Issues come from the forms under `.github/ISSUE_TEMPLATE/` (blank issues are off) and ask for
  the affected FR/UC/BR IDs up front; pull requests follow `.github/pull_request_template.md`,
  whose AIUP block and per-repo checklist are ticked or struck through with a reason, never
  left blank. All six repos' templates are generated by `generateGithubTemplates.py`; edit
  its table and rerun it rather than editing a template by hand.
- The hook's override, `ELICIT_ALLOW_PROTECTED=1`, is for a human at the keyboard. Never
  set it yourself; if the guard stops you, make a branch.
- CI per repository: Survey, Admin and Author run the Maven suite (`ci.yml`), FHHS runs it
  from `maven.yml`, Pedigree builds its image and smoke-tests `/healthz` and `/svg`, and the
  umbrella runs `checkLanguages.sh`, `check-properties.sh`, `docker compose config` and the
  manual build against fresh clones of Survey, Admin and Author at the target branch (Author
  at `main`).

## Git Commits

- Never append a `Co-Authored-By: Claude` (or similar AI co-author) trailer to
  commit messages, and no "Generated with Claude Code" footer on PR bodies.
