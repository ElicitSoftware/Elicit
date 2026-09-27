#!/bin/bash
# Stop the stacks: ./down.sh [usa|mexico|arabia|all]  (default all; the remote sites first,
# because their Admin containers are attached to USA's network).
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
done
