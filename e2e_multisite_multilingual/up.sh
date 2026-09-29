#!/bin/bash
#
# Bring up the three multilingual e2e stacks.
#
#   ./up.sh              all three (USA first: Mexico and Arabia use its Keycloak and Mailpit)
#   ./up.sh usa          USA only
#   ./up.sh mexico       Mexico only (USA must already be up)
#   ./up.sh arabia       Arabia only (USA must already be up)
#
# Waits until each site's Survey and Admin answer their readiness probes. Languages are packaged
# in the images; each site's compose file names the ones it offers in i18n.bundled.locales, so
# there is nothing to lay out first. A first start initializes the
# database in one pass (Survey creates the schema; nothing seeds a survey or a department -- the
# journey authors, applies and creates its own), which takes a minute or two per site.

set -euo pipefail
cd "$(dirname "$0")"

usage() { echo "Usage: $0 [usa|mexico|arabia|all]" >&2; exit 1; }

SITES=${1:-all}
case "$SITES" in
    all) SITES="usa mexico arabia" ;;
    usa|mexico|arabia) ;;
    *) usage ;;
esac

# host ports per site: survey admin
ports_for() {
    case "$1" in
        usa)    echo "8080 8081" ;;
        mexico) echo "8030 8031" ;;
        arabia) echo "7980 7981" ;;
    esac
}

# The glitch shows itself only after initdb has finished and PostgreSQL restarts on the freshly
# populated directory, and that first pass can take several minutes -- so this waits for one of the
# two outcomes rather than for a fixed minute (a one-minute window let a Mexico start slip through
# and hang the whole run on a database that had already exited).
wrong_ownership() { # site
    local i
    for i in $(seq 1 60); do
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
    for i in $(seq 1 72); do
        if curl -fsS -o /dev/null "http://localhost:$port/q/health/ready" 2>/dev/null; then
            echo "  ✅ $name ready on :$port"
            return 0
        fi
        sleep 5
    done
    echo "  ❌ $name did not become ready on :$port within 6 minutes" >&2
    docker compose -f "$1/docker-compose.yml" ps >&2
    return 1
}

for site in $SITES; do
    if [ "$site" != usa ]; then
        if ! docker network inspect elicit-ml-usa_default >/dev/null 2>&1 || \
           ! curl -fsS -o /dev/null http://localhost:8180/ 2>/dev/null; then
            echo "$site needs USA running (its Keycloak on :8180 and its compose network); run '$0 usa' first" >&2
            exit 1
        fi
    fi
    echo "== $site: docker compose up -d"
    mkdir -p "data/$site"
    docker compose -f "$site/docker-compose.yml" up -d || true
    # Docker Desktop for Mac (VirtioFS) can fail PostgreSQL's very first start on a fresh bind
    # mount with `data directory ... has wrong ownership` (docker/for-mac#7415, see
    # ../resetDatabase.sh). The cure is to wipe the half-initialized directory and start again.
    if wrong_ownership "$site"; then
        echo "  ⚠️  $site db hit the VirtioFS 'wrong ownership' glitch on first start; wiping PGDATA and retrying once"
        docker compose -f "$site/docker-compose.yml" down
        rm -rf "data/$site/PGDATA"
        docker compose -f "$site/docker-compose.yml" up -d
    fi
    read -r survey_port admin_port <<<"$(ports_for "$site")"
    wait_ready "$site" "$survey_port" "$site survey"
    wait_ready "$site" "$admin_port" "$site admin"
    if [ "$site" = usa ]; then
        wait_ready usa 8085 "usa author preview"
        wait_ready usa 8084 "usa author"
    fi
done
echo "Done. ./status.sh shows every URL."
