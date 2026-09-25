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
| `Admin/`      | `ElicitSoftware/Admin`       | Administrator app (Quarkus + Vaadin)            |
| `Author/`     | `ElicitSoftware/Author`      | Survey authoring tool (Quarkus + Vaadin)        |
| `FHHS/`       | `ElicitSoftware/FHHS`        | Family Health History Survey; headless REST     |
| `Pedigree/`   | `ElicitSoftware/Pedigree`    | R/Kinship2 pedigree-drawing service (plumber)   |
| `Prometheus/` | *tracked here*               | Prometheus config; not a repo, not in compose   |
| `postgresql/` | *not tracked, not a repo*    | Local `PGDATA` volume only                      |

`cloneAllProjects.sh` bootstraps all five module clones.

### Working Across Modules

- Edits inside a module directory belong to **that module's repo** — commit,
  branch, and open PRs there, not here.
- Modules routinely sit on **different branches** at the same time. Check
  `git -C <module> rev-parse --abbrev-ref HEAD` before starting; never assume
  they are all on `main`.
- A cross-cutting change (a schema change touching Survey + Admin + FHHS, say)
  needs a branch and a commit per repo. Confirm the branch/PR strategy with
  the user before committing across repos.
- The four Java modules each carry their own `CLAUDE.md` and `AGENTS.md` with
  the conventions that govern them. Those take precedence inside their
  subtree. `Pedigree/` and `Prometheus/` have no such file.

## Terminology: Access Code vs. Token

The credential a respondent enters to reach a survey is the **access code**
(`survey.respondents.access_code`, `accessCode` in Java, `<ACCESS_CODE>` in email
templates, `{AccessCode}` in the FHHS SFTP XML template). "Token" means only a
question-text placeholder (`{KEY|default}`, `survey.relationships.token`) or an
OIDC/Bearer token. Never call the respondent credential a token.

## Module Conventions (shared by Survey, Admin, Author, FHHS)

- Java 25, Quarkus 3.39.2, Maven. Vaadin 25.2.7 Flow for the three UI apps;
  FHHS is headless REST.
- **Hibernate ORM with Panache — not jOOQ.** Flyway migrations live under
  `src/main/resources/db/migration`.
- All four follow the **AI Unified Process** (AIUP): `docs/` is the source of
  truth for behavior, `src/` is the implementation. The `aiup-core` plugin
  supplies the skills (`/requirements`, `/entity-model`, `/use-case-diagram`,
  `/use-case-spec`, `/reverse-engineer`).
- They share one PostgreSQL database and the `survey` schema, which the Survey
  app owns.
- Use the **Quarkus Agent MCP tools** rather than raw `mvn` — these are
  existing projects, so start with `quarkus_update` and `quarkus_skills`. Do
  not run `mvn clean` while dev mode is running.

## Local Stack (`docker-compose.yml`)

Applications: `survey`, `admin`, `fhhs`, `pedigree`, `author`, and
`author-survey`. Supporting services: `db` (PostgreSQL), `keycloak` (OIDC),
`mailpit` (SMTP), `sftpServer`, and `jaeger` (OpenTelemetry). All app images
are built locally as `elicitsoftware/<name>:latest`.

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

`author-survey` is a second `elicitsoftware/survey:latest` container serving
Author's preview: both its datasources point at the `author` database rather than
`survey`, `accessCode.autoRegister` lets an author type any access code to walk a
draft, and `elicit.etl.enabled=false` keeps it from touching the reporting schema
(Author's database holds many surveys whose step names would collide in
`surveyreport.dim_step`).

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
`FHHS/family-history-survey.elicit`. The apply asks Survey to rebuild the reporting star
schema (`POST /api/etl/build`), so **no Survey restart is needed** and `surveyreport`
grows from its six skeleton tables to the full set (18 dimension tables, two fact tables,
`fact_respondents_view` / `fact_sections_view`) as part of the apply. Confirm with:

```sh
docker exec elicit-db-1 psql -U survey -d survey \
  -c "select count(*) from surveyreport.dim_step;"
```

which must be non-zero (15 for the Family History Survey).

FHHS is specific to that survey (FHHS UC-005). Until it is imported, FHHS starts, logs one
WARN naming the survey key and the import to perform, reports **not-ready** on
`/q/health/ready`, and answers report requests with 503 carrying the same message. It goes
healthy by itself on the next probe after the import; nothing needs restarting. Admin
depends on FHHS with `service_started`, not `service_healthy`, so Admin comes up either
way and the import is always reachable. `deploy.sh` (`up -d`, then restart Survey) is only
needed on a stack whose survey was seeded before this change.

The fact tables stay empty until respondents exist; they fill through the
`fact_respondent_insert` and `fact_update` triggers on `survey.respondents`.

`resetDatabase.sh V3` stops the stack and deletes `postgresql/PGDATA` for a
greenfield run. `resetDatabase.sh V2` replaces it with a copy of `PGDATA_v2`, the
pre-Kimball V2 database, for a brownfield (upgrade) run. `PGDATA_v2` is tracked in
git, which drops the empty directories PostgreSQL needs (`pg_notify`, `pg_tblspc`
and eleven more), so the V2 mode recreates them in the copy; never copy it by hand.

If the db container's first start after a reset fails with `data directory
"/var/lib/postgresql/data/pgdata" has wrong ownership`, that is a Docker Desktop
for Mac VirtioFS glitch (docker/for-mac#7415), not a broken copy: bind mounts show
every file as root-owned inside the container and PostgreSQL's owner check can
fail on initial startup. Run `resetDatabase.sh` again and restart; if it recurs,
switch Docker Desktop's file sharing to gRPC FUSE. Both reset paths were verified
to start cleanly on 2026-09-20.

## Scripts

- `cloneAllProjects.sh` — clone the five module repos.
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
  a dev-mode Survey on 8080 hangs the Survey test suite, because the test data points
  post-survey-action and report URLs at `localhost:8080`, dev mode parks requests while
  it restarts, and neither Survey HTTP client sets a timeout.
- `deploy.sh` — bring up an initialized stack.
- `resetDatabase.sh V2|V3` — reset `postgresql/PGDATA` to the V2 copy (brownfield)
  or delete it (greenfield); stops the stack first.
- `status.sh` — container status plus an HTTP probe of each service.

## Branding

`elicit-brand/` is the default brand (colors, typography, `theme.css`,
`brand-config.json`), mounted read-only at `/opt/brand` in the Survey and
Admin containers via `brand.file.system.path`. `test-brand/` and
`test-partial-brand/` exercise fallback behavior when a brand omits assets.
See `docs/BRAND_SYSTEM_IMPLEMENTATION_GUIDE.md`.

## Translations (i18n)

`elicit-i18n/` is the default translations mount, mounted read-only at `/opt/i18n`
(`i18n.file.system.path`) in the Survey, Admin, Author and author-survey containers with
one sub-directory per app (`survey/`, `admin/`, `author/`). The apps ship English only and hide
the language selector until a second language is mounted; `elicit-i18n` holds the Spanish
(`es-419`) and Arabic (`ar`) files, and the module test and dev profiles read it as `../elicit-i18n`,
so the module language tests need this umbrella checkout. Each app resolves a key
through classpath `vaadin-i18n/translations[_tag].properties` → local `i18n/<app>/` →
the mount, per key, falling back to English; a language that exists only on the mount
is offered too. `test-partial-i18n/` exercises the override and mount-only paths.
Direction (RTL/LTR) follows the language, with `i18n-config.json` as an optional
override. Brand names are translated in the brand's own `localized` block, never in the
app bundles. Survey content in the database is not translated by this mechanism. See
`docs/I18N_IMPLEMENTATION_GUIDE.md`.

## Umbrella Docs

The umbrella now carries its own AIUP artifacts — the first ones it has had —
scoped to the platform as one deployable whole: `docs/vision.md`,
`docs/requirements.md` (FR-001..035, NFR-001..010, C-001..014),
`docs/use_cases.puml` and `docs/use_cases/UC-001..019`. Module requirement and
use-case IDs are unrelated to these.

- `docs/manual/` — the **installation manual** (UC-001), a LaTeX/PDF that covers
  Survey, Admin and Author: topology, prerequisites, the database roles and
  schemas, the OIDC clients and roles, both installation paths, the first
  sign-in, the configuration reference, branding, translations and verification.
  Built by `docs/manual/build-manual.sh` through a TeX Live container, stamped
  with the version the three modules agree on, and built alongside the images by
  `buildDockerImages.sh` (target `Manual`). Unlike the author's manual it is
  **not** packaged into any image — an operator reads it before any Elicit
  service exists. `docs/manual/check-properties.sh` gates the configuration
  reference against the modules' `@ConfigProperty` declarations and
  `application.properties`; `buildDockerImages.sh` runs it before typesetting, so
  a drifted reference fails the build rather than reaching a release.
- `DeploymentScript.md` — non-Docker deployment, plus the procedure for
  upgrading an existing deployment to Kimball Type 2 SCD (V3.0.0). Upgrade
  procedures stay here and are deliberately **not** in the installation manual,
  which is stamped with one version and outlives it.
- `docs/BRAND_SYSTEM_IMPLEMENTATION_GUIDE.md`
- `docs/I18N_IMPLEMENTATION_GUIDE.md`
- `docs/metrics/OBSERVABILITY_IMPLEMENTATION_GUIDE.md`,
  `docs/metrics/PROMETHEUS_QUERIES.md`

## Git Commits

- Never append a `Co-Authored-By: Claude` (or similar AI co-author) trailer to
  commit messages, and no "Generated with Claude Code" footer on PR bodies.
