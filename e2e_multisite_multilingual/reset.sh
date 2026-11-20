#!/bin/bash
#
# Wipe a site for a greenfield rerun: ./reset.sh [usa|mexico|arabia|all]  (default all).
# Stops the site and deletes data/<site> (PostgreSQL PGDATA, and for USA the Mailpit database and
# the Keycloak H2 copy). The next ./up.sh initializes the database from scratch.
#
# The journey installs the one Census Household Survey -- one survey key across both revisions and
# all three sites -- and phase 1 refuses to start if Author already holds it, so the databases must
# be clear every time.
#
# If the first start after a reset dies with
#   FATAL:  data directory "/var/lib/postgresql/data/pgdata" has wrong ownership
# that is the Docker Desktop for Mac VirtioFS glitch described in ../resetDatabase.sh; run this
# script again and restart.
set -euo pipefail
cd "$(dirname "$0")"
case "${1:-all}" in
    all) SITES="arabia mexico usa" ;;
    usa|mexico|arabia) SITES=$1 ;;
    *) echo "Usage: $0 [usa|mexico|arabia|all]" >&2; exit 1 ;;
esac
for site in $SITES; do
    echo "== $site: docker compose down"
    docker compose -f "$site/docker-compose.yml" down
    echo "== $site: removing data/$site"
    # Twice, because of Finder. macOS drops a .DS_Store into a directory it is looking at, and it
    # can land between rm emptying the directory and rm removing it -- so the first pass dies with
    # "Directory not empty" on a directory it had just emptied, and under `set -e` that fails the
    # whole run. The second pass finds only the .DS_Store and succeeds. Failing after that is real.
    rm -rf "data/$site" 2>/dev/null || rm -rf "data/$site"
done
