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
- Use the **Quarkus, Vaadin, and IntelliJ MCP servers and skills** rather
  than raw `mvn`, web search, or recalled API knowledge — see the next section.

## MCP Servers and Skills

Three MCP servers and their skill packs are configured for this workspace.
Reach for them before falling back to raw Maven, grep, web search, or memory
of an API. Each Java module's `CLAUDE.md` repeats the module-specific parts.

### Quarkus — `quarkus-agent` plugin (Survey, Admin, Author, FHHS)

- Every module is an **existing project**: begin with `quarkus_update`, then
  `quarkus_skills` for each extension you are about to touch. Never
  `quarkus_create`.
- `quarkus_searchDocs` for Quarkus configuration and APIs — not Context7, not
  web search.
- `quarkus_start` / `quarkus_stop` / `quarkus_status` / `quarkus_logs` manage
  dev mode. Reload after code changes with `quarkus_callTool` →
  `devui-logstream_forceRestart`; after a `pom.xml` change do a full
  stop/start.
- Run tests with `quarkus_callTool` → `devui-testing_runTests` (or
  `devui-testing_runTest` with a class name). Do not run `mvn test`, and never
  `mvn clean` while dev mode is running.
- `quarkus_searchTools` discovers the Dev MCP tools on the running app; the
  list changes when extensions change.
- Each module's `AGENTS.md` carries the full Quarkus-agent workflow.

### Vaadin — `vaadin-skills` plugin (Survey, Admin, Author; not FHHS)

- Before writing Flow code against 25.2, call `get_vaadin_primer` and
  `get_new_apis` — API added after the model's training data is API it cannot
  know it is missing.
- Call `get_java_symbol` before concluding a method or enum constant does not
  exist; it lists inherited members too.
- `search_vaadin_docs` (ui_language `java`, vaadin_version `25.2`) for
  guides; `get_component_java_api`, `get_component_styling`, and
  `get_theme_css_properties` for a component's API and CSS.
- Skills: `/vaadin-form-layout` for forms and entity editors,
  `/vaadin-frontend-design` for polished views, `/aura-theme` for theme CSS.
- Never target 25.3; it is unreleased.

### IntelliJ IDEA — `idea` MCP server (all modules)

- The umbrella `.run/` holds a Quarkus dev-mode run configuration per Java
  module (`Survey`, `Admin`, `Author`, `FHHS`), and each module carries its own
  copy. The local workflow is: support services in Docker Compose, the module
  under development stopped in Docker and launched via
  `execute_run_configuration` (list them with `get_run_configurations`).
- Prefer IDE-backed navigation and checks over grep when the workspace is
  open: `search_symbol`, `get_symbol_info`, `analyze_calls`,
  `get_file_problems`, `lint_files`, `rename_refactoring`, `reformat_file`,
  `build_project`.
- The database tools (`list_database_connections`, `introspect_schema`,
  `execute_sql_query`, `preview_table_data`) can inspect the shared `survey`
  schema on the local stack (host port `5452`).
- The `xdebug_*` tools set breakpoints and step through a running debug
  session.

## Local Stack (`docker-compose.yml`)

Applications: `survey`, `admin`, `fhhs`, `pedigree`, `author`. Supporting services:
`db` (PostgreSQL), `keycloak` (OIDC), `mailpit` (SMTP), `sftpServer`, and
`jaeger` (OpenTelemetry). All app images are built locally as
`elicitsoftware/<name>:latest`.

| Port    | Service                                    |
| ------- | ------------------------------------------ |
| `8080`  | Survey                                     |
| `8081`  | Admin                                      |
| `8082`  | FHHS                                       |
| `8083`  | Pedigree                                   |
| `8084`  | Author                                     |
| `8180`  | Keycloak (admin/admin)                     |
| `8025`  | Mailpit web UI                             |
| `16686` | Jaeger UI                                  |
| `5452`  | PostgreSQL (host) → `5432` in container    |

Healthchecks target the **container-internal** port `8080`, not the published
host port. Every service exports OTLP traces/metrics/logs to `jaeger:4317`.

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

### Startup Ordering Is Not Idempotent

First-run initialization requires **three passes**. Survey creates the schema;
FHHS then inserts survey data and tries to build reporting views over
dimensional tables that do not exist yet, so it fails; restarting Survey
creates those tables; restarting FHHS finally builds the views:

```
docker compose up -d && sleep 25 && docker compose restart && sleep 25 \
  && docker compose restart && sleep 25
```

An FHHS container that exits on the first pass is expected, not a bug.
`deploy.sh` is the short form for an already-initialized stack (`up -d`, then
restart Survey). To reset the database, stop the stack and delete
`postgresql/PGDATA`.

## Scripts

- `cloneAllProjects.sh` — clone the five module repos.
- `buildDockerImages.sh` — build the module images in dependency order:
  Survey → FHHS → Pedigree → Admin → Author. The PREMM5 step is commented out
  (that module is not cloned and is commented out of compose), and there is no
  `postgresql/` build step — `elicitsoftware/elicit_db` is pulled, not built.
- `deploy.sh` — bring up an initialized stack.
- `status.sh` — container status plus an HTTP probe of each service.

## Branding

`elicit-brand/` is the default brand (colors, typography, `theme.css`,
`brand-config.json`), mounted read-only at `/opt/brand` in the Survey and
Admin containers via `brand.file.system.path`. `test-brand/` and
`test-partial-brand/` exercise fallback behavior when a brand omits assets.
See `docs/BRAND_SYSTEM_IMPLEMENTATION_GUIDE.md`.

## Umbrella Docs

- `DeploymentScript.md` — non-Docker deployment, plus the procedure for
  upgrading an existing deployment to Kimball Type 2 SCD (V3.0.0).
- `docs/BRAND_SYSTEM_IMPLEMENTATION_GUIDE.md`
- `docs/metrics/OBSERVABILITY_IMPLEMENTATION_GUIDE.md`,
  `docs/metrics/PROMETHEUS_QUERIES.md`

## Git Commits

- Never append a `Co-Authored-By: Claude` (or similar AI co-author) trailer to
  commit messages, and no "Generated with Claude Code" footer on PR bodies.
