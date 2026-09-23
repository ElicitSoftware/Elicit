#!/bin/bash
#
# Wipe a site for a greenfield rerun: ./reset.sh [site1|site2|all]  (default all).
# Stops the site and deletes data/<site> (PostgreSQL PGDATA, Mailpit, SFTP uploads, the
# Keycloak H2 copy). The next ./up.sh initialises the database from scratch.
#
# If the first start after a reset dies with
#   FATAL:  data directory "/var/lib/postgresql/data/pgdata" has wrong ownership
# that is the Docker Desktop for Mac VirtioFS glitch described in ../resetDatabase.sh; run this
# script again and restart.
set -euo pipefail
cd "$(dirname "$0")"
case "${1:-all}" in
    all) SITES="site2 site1" ;;
    site1|site2) SITES=$1 ;;
    *) echo "Usage: $0 [site1|site2|all]" >&2; exit 1 ;;
esac
for site in $SITES; do
    echo "== $site: docker compose down"
    docker compose -f "$site/docker-compose.yml" down
    echo "== $site: removing data/$site"
    rm -rf "data/$site"
done
