# Elicit Analytics — Apache Superset

Apache Superset (Apache-2.0) is the platform's analytics tool. It reads the `surveyreport`
star schema as the read-only `surveyreport_user`, signs users in through Keycloak, and is
embedded in the Admin console's Analytics view. Design and decisions:
`Admin/docs/research/Superset.md` and `Admin/docs/plan/superset-analytics-implementation.md`.

## What runs

| Service | Role |
|---|---|
| `superset-init` | Run-once job: creates the `superset` metadata database and role, runs `superset db upgrade` / `superset init`, creates the local `admin` user, runs `bootstrap.py`, imports every ZIP under `assets/`, runs `bootstrap.py` again. Exits 0 when done. |
| `superset` | The web app on http://localhost:8088. |
| `superset-worker`, `superset-beat` | Celery worker and scheduler for Alerts & Reports, thumbnails, and server-side screenshots (Playwright + Chromium). |
| `redis` | Cache and Celery broker. |

Build the image with `./buildDockerImage.sh` (also run by the umbrella `buildDockerImages.sh`).

## Files

| File | Purpose |
|---|---|
| `Dockerfile` | `apache/superset:6.1.0` plus Authlib, Pillow, Playwright/Chromium, psycopg2 |
| `superset_config.py` | All configuration; every deployment value comes from the environment (see `docker-compose.yml`) |
| `elicit_security.py` | Maps `resource_access.elicit-superset.roles` from Keycloak's userinfo to Superset roles via `AUTH_ROLES_MAPPING` |
| `bootstrap.py` | Idempotent: reporting connection (fixed UUID), `ElicitAnalyst` and `ElicitGuest` roles, datasource access, dashboard sharing, embedded ids |
| `init.sh` | The `superset-init` entrypoint |
| `assets/*.zip` | Dashboard exports imported at start (see below) |

## Roles

| Keycloak client role (`elicit-superset`) | Superset role | Gets |
|---|---|---|
| `elicit_analytics` | `ElicitAnalyst` | Gamma minus SQL Lab, datasource access on the reporting datasets, the shipped dashboards |
| `elicit_superset_admin` | `Admin` | Everything (authoring) |
| *(none)* | `Public` | Nothing |

Guest tokens minted by Admin for the embedded view run as `ElicitGuest` (same data access as
an analyst, restricted to the dashboard named in the token).

## Authoring and shipping a dashboard

1. Sign in at http://localhost:8088 as a user holding `elicit_superset_admin` (locally `admin`).
2. Build datasets, charts, and a dashboard on the **Elicit Reporting** connection. Give the
   dashboard a stable **slug** (`survey-operations` for the shipped one).
3. Export it: *Dashboards → ⋮ → Export* (or `GET /api/v1/dashboard/export/?q=!(<id>)`), and
   save the ZIP under `assets/`.
4. Rebuild the image and re-run `superset-init`
   (`docker compose up -d --build superset-init`). Import is idempotent with overwrite.

Embedding is not part of the export. `bootstrap.py` gives each dashboard listed in its
`EMBEDDED_DASHBOARDS` map a fixed embedded UUID and the allowed domains from
`SUPERSET_FRAME_ANCESTORS`; Admin's `SUPERSET_DASHBOARD_ID` must be that UUID.

## Environment (see `docker-compose.yml`)

`SUPERSET_SECRET_KEY`, `SUPERSET_DB_URI`, `SUPERSET_REDIS_URL`, `SUPERSET_PUBLIC_URL`,
`SUPERSET_INTERNAL_URL`, `SUPERSET_FRAME_ANCESTORS` (full origins, e.g. `http://localhost:8081`;
used both for the CSP and as the embedded dashboards' allowed domains),
`SUPERSET_GUEST_TOKEN_SECRET` and `SUPERSET_GUEST_TOKEN_AUDIENCE` (both shared with Admin;
Superset 6 rejects a guest token without an audience), `SUPERSET_REPORTING_DB_URI`, `OIDC_SERVER_URL`, `OIDC_CLIENT_ID`,
`OIDC_CLIENT_SECRET`, `SUPERSET_SMTP_HOST`/`PORT`, and for init only
`ELICIT_OWNER_DB_PASSWORD`, `SUPERSET_ADMIN_PASSWORD`.

## Notes

- The OIDC URL follows the same rule as Admin and Author: `host.docker.internal:8180`
  resolves from the container and from the browser on Docker Desktop.
- Keycloak's `--import-realm` skips a realm that already exists. After a realm change,
  recreate the Keycloak container (or reset its database) or the `elicit-superset` client
  will be missing and single sign-on will fail with "client not found".
- Local HTTP: guest tokens travel in the `X-GuestToken` header, so no cross-site cookie is
  needed for embedding. Behind HTTPS, keep `SUPERSET_PUBLIC_URL` on `https://` so the
  session cookie is marked secure.
