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
#
# PGDATA_v2 was also initialised on a host whose glibc (2.41) is newer than the one in the
# elicitsoftware/elicit_db image (2.36), so PostgreSQL warns of a collation version
# mismatch on every connection, and an index on a text column built under one glibc can
# order differently under the other. The V2 mode therefore starts the database once,
# rebuilds the indexes of every database whose recorded collation version differs from
# the image's, records the new version (PostgreSQL's documented remedy), and stops it
# again. A real site whose database host's glibc changes needs the same two statements;
# see DeploymentScript.md.
#
# PGDATA_v2 also predates Author's database, and db-init/V0.0.4__CREATE_AUTHOR_DATABASE.sql
# runs only when a cluster is first initialized, so without it author-survey fails with
# `database "author" does not exist` and Author never starts. While the database is up for
# the collation repair, the V2 mode runs that script too, as the script's own header tells
# an operator to do by hand on an existing cluster.
#
# If the first start after a reset dies with
#   FATAL:  data directory "/var/lib/postgresql/data/pgdata" has wrong ownership
# that is a Docker Desktop for Mac VirtioFS glitch, not a problem with the copy: bind
# mounts report every file as root-owned inside the container and PostgreSQL's owner
# check occasionally fails on initial startup (docker/for-mac#7415). Run this script
# again and restart the stack; if it keeps happening, switch Docker Desktop's file
# sharing implementation to gRPC FUSE.

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

AUTHOR_DB_SCRIPT=/docker-entrypoint-initdb.d/V0.0.4__CREATE_AUTHOR_DATABASE.sql

# REINDEX, then REFRESH COLLATION VERSION, for each database whose recorded collation
# version differs from the one the running image provides. Run against a started db.
refresh_collation_versions() {
    local mismatched db
    mismatched=$(db_psql postgres "select datname from pg_database
        where datallowconn
          and datcollversion is distinct from pg_database_collation_actual_version(oid)")
    for db in $mismatched; do
        echo "  $db: rebuilding indexes and refreshing the collation version"
        db_psql "$db" "REINDEX DATABASE \"$db\""
        db_psql postgres "ALTER DATABASE \"$db\" REFRESH COLLATION VERSION"
    done
}

# Create Author's database from the script compose mounts into the db service, unless the
# cluster already has one. Run against a started db.
create_author_database() {
    if [ -n "$(db_psql postgres "select 1 from pg_database where datname = 'author'")" ]; then
        echo "  author: already present"
        return
    fi
    echo "  author: creating it from $AUTHOR_DB_SCRIPT"
    docker compose exec -T db psql -U survey -d survey -v ON_ERROR_STOP=1 -X -q -f "$AUTHOR_DB_SCRIPT"
}

# psql against the db service. The collation mismatch warning PostgreSQL prints on every
# connection is the condition being fixed, so its three lines are dropped; anything else,
# errors included, is shown, and a failing statement fails the script (pipefail).
db_psql() {
    docker compose exec -T db psql -U survey -d "$1" -v ON_ERROR_STOP=1 -X -q -At -c "$2" 2>&1 |
        { grep -vE '^(WARNING:  database ".*" has a collation version mismatch|DETAIL:  The database was created using collation version|HINT:  Rebuild all objects in this database)' || true; }
}

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
        echo "Matching the copy's collation version to the database image"
        docker compose up -d --wait db
        refresh_collation_versions
        echo "Adding Author's database, which the V2 cluster predates"
        create_author_database
        docker compose stop db
        echo "Done. 'docker compose up -d' will run the V2 -> V3 upgrade (brownfield) path."
        ;;
    *)
        usage
        ;;
esac
