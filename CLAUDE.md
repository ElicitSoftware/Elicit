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

### First Run Is a Single Pass

`docker compose up -d` initializes a fresh database in one pass: Survey creates
the schema, then FHHS (which waits for Survey to be healthy) seeds the Family
History Survey. FHHS's greenfield migrations use literal ids and fixed keys and no
longer build anything over the ETL-generated reporting views, so no restart is
needed and a failed attempt can simply be retried. `deploy.sh` is the short form
for an already-initialized stack (`up -d`, then restart Survey).

`resetDatabase.sh V3` stops the stack and deletes `postgresql/PGDATA` for a
greenfield run. `resetDatabase.sh V2` replaces it with a copy of `PGDATA_v2`, the
pre-Kimball V2 database, for a brownfield (upgrade) run. `PGDATA_v2` is tracked in
git, which drops the empty directories PostgreSQL needs (`pg_notify`, `pg_tblspc`
and eleven more), so the V2 mode recreates them in the copy; never copy it by hand.

## Scripts

- `cloneAllProjects.sh` — clone the five module repos.
- `buildDockerImages.sh` — build the module images in dependency order:
  Survey → FHHS → Pedigree → Admin → Author. The PREMM5 step is commented out
  (that module is not cloned and is commented out of compose), and there is no
  `postgresql/` build step — `elicitsoftware/elicit_db` is pulled, not built.
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

## Umbrella Docs

- `DeploymentScript.md` — non-Docker deployment, plus the procedure for
  upgrading an existing deployment to Kimball Type 2 SCD (V3.0.0).
- `docs/BRAND_SYSTEM_IMPLEMENTATION_GUIDE.md`
- `docs/metrics/OBSERVABILITY_IMPLEMENTATION_GUIDE.md`,
  `docs/metrics/PROMETHEUS_QUERIES.md`

## Git Commits

- Never append a `Co-Authored-By: Claude` (or similar AI co-author) trailer to
  commit messages, and no "Generated with Claude Code" footer on PR bodies.
