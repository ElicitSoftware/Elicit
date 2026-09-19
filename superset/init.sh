#!/usr/bin/env bash
# One-shot initialisation for the Elicit Superset deployment (the superset-init service).
# Safe to re-run: every step is idempotent. Expected to exit 0 and stop.
set -euo pipefail

echo "init: waiting for the database, then ensuring the metadata database exists"
python - <<'PY'
import os, time, psycopg2
from urllib.parse import urlparse
uri = urlparse(os.environ["SUPERSET_DB_URI"])
owner = None
for attempt in range(60):   # a compose restart cycles db underneath us; wait up to ~3 minutes
    try:
        owner = psycopg2.connect(host=uri.hostname, port=uri.port or 5432, dbname="survey",
                                 user=os.environ.get("ELICIT_OWNER_DB_USER", "elicit_owner"),
                                 password=os.environ["ELICIT_OWNER_DB_PASSWORD"], connect_timeout=5)
        break
    except psycopg2.OperationalError as e:
        print(f"init: database not ready ({str(e).strip().splitlines()[-1]}); retrying")
        time.sleep(3)
if owner is None:
    raise SystemExit("init: database never became ready")
owner.autocommit = True
cur = owner.cursor()
cur.execute("SELECT 1 FROM pg_roles WHERE rolname = %s", (uri.username,))
if cur.fetchone() is None:
    cur.execute(f"CREATE ROLE \"{uri.username}\" LOGIN PASSWORD %s", (uri.password,))
    print(f"init: created role {uri.username}")
cur.execute("SELECT 1 FROM pg_database WHERE datname = %s", (uri.path.lstrip('/'),))
if cur.fetchone() is None:
    cur.execute(f"CREATE DATABASE \"{uri.path.lstrip('/')}\" OWNER \"{uri.username}\"")
    print(f"init: created database {uri.path.lstrip('/')}")
owner.close()
PY

echo "init: superset db upgrade"
superset db upgrade
echo "init: superset init"
superset init

echo "init: ensuring the local admin user exists (owns imported assets; Keycloak admin maps to it by username)"
superset fab create-admin \
  --username "${SUPERSET_ADMIN_USERNAME:-admin}" --firstname Admin --lastname Elicit \
  --email "${SUPERSET_ADMIN_EMAIL:-admin@temp.org}" --password "${SUPERSET_ADMIN_PASSWORD:-admin}" \
  >/dev/null 2>&1 || echo "init: admin user already present"

echo "init: bootstrap (reporting connection, roles)"
python /app/elicit/bootstrap.py

shopt -s nullglob
for zip in /app/elicit/assets/*.zip; do
  echo "init: importing $zip"
  superset import-dashboards -p "$zip" -u "${SUPERSET_ADMIN_USERNAME:-admin}"
done

echo "init: bootstrap (datasource access for imported datasets)"
python /app/elicit/bootstrap.py
echo "init: done"
