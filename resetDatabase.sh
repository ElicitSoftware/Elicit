#!/bin/bash
#
# Reset the local PostgreSQL data directory for a greenfield or a brownfield run.
#
#   ./resetDatabase.sh V3   Delete postgresql/PGDATA. The next `docker compose up -d`
#                           initializes a fresh database (greenfield path).
#   ./resetDatabase.sh V2   Replace postgresql/PGDATA with a copy of PGDATA_v2, the
#                           pre-Kimball V2 database, so the next `docker compose up -d`
#                           exercises the upgrade (brownfield) path.
#
# PGDATA_v2 is tracked in git, and git cannot store empty directories, so a checkout is
# missing the directories PostgreSQL requires (it refuses to start with "could not open
# directory pg_notify"). Placeholder files are not an option because PostgreSQL reads
# every entry of pg_tblspc and pg_replslot as data, so the V2 mode recreates the
# directories in the copy instead.

set -euo pipefail
cd "$(dirname "$0")"

PGDATA_DIR=postgresql/PGDATA
V2_SOURCE=PGDATA_v2

# Directories that are empty in a healthy PG17 cluster and therefore dropped by git.
EMPTY_CLUSTER_DIRS=(
    pg_commit_ts
    pg_dynshmem
    pg_notify
    pg_replslot
    pg_serial
    pg_snapshots
    pg_stat_tmp
    pg_tblspc
    pg_twophase
    pg_logical/mappings
    pg_logical/snapshots
    pg_wal/archive_status
    pg_wal/summaries
)

usage() {
    echo "Usage: $0 V2|V3" >&2
    echo "  V3  delete $PGDATA_DIR (greenfield: next start initializes a fresh database)" >&2
    echo "  V2  copy $V2_SOURCE to $PGDATA_DIR and add the empty cluster directories (brownfield)" >&2
    exit 1
}

[ $# -eq 1 ] || usage
MODE=$(echo "$1" | tr '[:lower:]' '[:upper:]')

case "$MODE" in
    V3)
        echo "Stopping the stack"
        docker compose down
        echo "Deleting $PGDATA_DIR"
        rm -rf "$PGDATA_DIR"
        echo "Done. 'docker compose up -d' will initialize a fresh (greenfield) database."
        ;;
    V2)
        [ -d "$V2_SOURCE/pgdata" ] || { echo "$V2_SOURCE/pgdata not found" >&2; exit 1; }
        echo "Stopping the stack"
        docker compose down
        echo "Replacing $PGDATA_DIR with a copy of $V2_SOURCE"
        rm -rf "$PGDATA_DIR"
        cp -R "$V2_SOURCE" "$PGDATA_DIR"
        echo "Adding the empty cluster directories git does not keep"
        for d in "${EMPTY_CLUSTER_DIRS[@]}"; do
            mkdir -p "$PGDATA_DIR/pgdata/$d"
            chmod 700 "$PGDATA_DIR/pgdata/$d"
        done
        echo "Done. 'docker compose up -d' will run the V2 -> V3 upgrade (brownfield) path."
        ;;
    *)
        usage
        ;;
esac
