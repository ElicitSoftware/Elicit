#!/bin/bash
# Stop the multi-site e2e stacks: ./down.sh [site1|site2|all]  (default all; site 2 first,
# because it is attached to site 1's network).
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
done
