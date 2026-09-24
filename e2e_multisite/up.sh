#!/bin/bash
#
# Bring up the multi-site e2e stacks.
#
#   ./up.sh            both sites (site 1 first, then site 2)
#   ./up.sh site1      site 1 only
#   ./up.sh site2      site 2 only (site 1 must already be up: site 2 uses its Keycloak and Mailpit)
#
# Waits until each site's Survey, FHHS and Admin answer their readiness probes, and on site 2
# applies site2/post-init.sql once FHHS has seeded its rows. A first start initialises the
# database in one pass (Survey creates the schema, FHHS seeds the Family History Survey); that
# takes two to three minutes per site.

set -euo pipefail
cd "$(dirname "$0")"

usage() { echo "Usage: $0 [site1|site2|all]" >&2; exit 1; }

SITES=${1:-all}
case "$SITES" in
    all) SITES="site1 site2" ;;
    site1|site2) ;;
    *) usage ;;
esac

# host ports per site: survey fhhs admin
ports_for() {
    case "$1" in
        site1) echo "8080 8082 8081" ;;
        site2) echo "8030 8032 8031" ;;
    esac
}

wrong_ownership() { # site
    local i
    for i in $(seq 1 12); do
        if docker compose -f "$1/docker-compose.yml" logs db 2>/dev/null | grep -q 'has wrong ownership'; then
            return 0
        fi
        if docker compose -f "$1/docker-compose.yml" ps --format '{{.Service}} {{.Status}}' 2>/dev/null | grep -qE '^db .*healthy'; then
            return 1
        fi
        sleep 5
    done
    return 1
}

wait_ready() { # site port name
    local port=$2 name=$3 i
    for i in $(seq 1 60); do
        if curl -fsS -o /dev/null "http://localhost:$port/q/health/ready" 2>/dev/null; then
            echo "  ✅ $name ready on :$port"
            return 0
        fi
        sleep 5
    done
    echo "  ❌ $name did not become ready on :$port within 5 minutes" >&2
    docker compose -f "$1/docker-compose.yml" ps >&2
    return 1
}

for site in $SITES; do
    if [ "$site" = site2 ]; then
        if ! docker network inspect elicit-site1_default >/dev/null 2>&1 || \
           ! curl -fsS -o /dev/null http://localhost:8180/ 2>/dev/null; then
            echo "site 2 needs site 1 running (its Keycloak on :8180 and its compose network); run '$0 site1' first" >&2
            exit 1
        fi
    fi
    echo "== $site: docker compose up -d"
    mkdir -p "data/$site"
    docker compose -f "$site/docker-compose.yml" up -d || true
    # Docker Desktop for Mac (VirtioFS) can fail PostgreSQL's very first start on a fresh bind
    # mount with `data directory ... has wrong ownership` (docker/for-mac#7415, see
    # ../resetDatabase.sh). The cure is to wipe the half-initialised directory and start again.
    if wrong_ownership "$site"; then
        echo "  ⚠️  $site db hit the VirtioFS 'wrong ownership' glitch on first start; wiping PGDATA and retrying once"
        docker compose -f "$site/docker-compose.yml" down
        rm -rf "data/$site/PGDATA"
        docker compose -f "$site/docker-compose.yml" up -d
    fi
    read -r survey_port fhhs_port admin_port <<<"$(ports_for "$site")"
    wait_ready "$site" "$survey_port" "$site survey"
    wait_ready "$site" "$fhhs_port" "$site fhhs"
    wait_ready "$site" "$admin_port" "$site admin"
    if [ "$site" = site2 ]; then
        echo "== site2: pointing report and post-survey-action URLs at site 2's FHHS"
        docker compose -f site2/docker-compose.yml exec -T db psql -v ON_ERROR_STOP=1 -U survey -d survey < site2/post-init.sql
    fi
done
echo "Done. ./status.sh shows every URL."
