#!/bin/bash
#
# Reset the stack's database volume for a greenfield or a brownfield run. The database lives
# in the named volume `pgdata` of docker-compose.yml (elicit_pgdata to Docker).
#
#   ./resetDatabase.sh V3   Delete the volume. The next `docker compose up -d` initializes a
#                           fresh database (greenfield path).
#   ./resetDatabase.sh V2   Replace the volume's content with a copy of PGDATA_v2, the
#                           pre-Kimball V2 database, so the next `docker compose up -d`
#                           exercises the upgrade (brownfield) path.
#
# Only the database is reset: the caught mail and the SFTP uploads keep their volumes
# (`docker compose down -v` deletes all three).
#
# PGDATA_v2 is tracked in git, and git cannot store empty directories, so a checkout is
# missing the directories PostgreSQL requires (it refuses to start with "could not open
# directory pg_notify"). Placeholder files are not an option because PostgreSQL reads
# every entry of pg_tblspc and pg_replslot as data, so the V2 mode recreates the
# directories in the copy instead. The copy is made by a one-off container of the db service,
# as root, and handed to the image's postgres user with the permissions PostgreSQL insists on.
#
# PGDATA_v2 was also initialized on a host whose glibc (2.41) is newer than the one in the
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
# PGDATA_v2 was, last, initialized by version 1.0.0 of the database image, whose schema owner
# (elicit_owner) and owner group (dbowner) are superusers. No role Elicit prescribes is one
# any longer (umbrella C-020), so the V2 mode narrows them with the three statements
# DeploymentScript.md gives a site in the same position, and the brownfield run upgrades the
# V2 database as a schema owner that is not a superuser.

set -euo pipefail
cd "$(dirname "$0")"

# The compose file names its project `elicit`, so this is the volume behind `pgdata`.
PGDATA_VOLUME=elicit_pgdata
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

# Create Author's database from the script compose places in the db service, unless the
# cluster already has one. Run against a started db.
create_author_database() {
    if [ -n "$(db_psql postgres "select 1 from pg_database where datname = 'author'")" ]; then
        echo "  author: already present"
        return
    fi
    echo "  author: creating it from $AUTHOR_DB_SCRIPT"
    docker compose exec -T db psql -U survey -d survey -v ON_ERROR_STOP=1 -X -q -f "$AUTHOR_DB_SCRIPT"
}

# Make the schema owner a member of the owner group, which holds CREATE on the database, and
# take the superuser attribute from both. Run against a started db.
narrow_owner_roles() {
    db_psql postgres "GRANT dbowner TO elicit_owner"
    db_psql postgres "ALTER ROLE elicit_owner NOSUPERUSER"
    db_psql postgres "ALTER ROLE dbowner NOSUPERUSER"
    local left
    left=$(db_psql postgres "select rolname from pg_roles
        where rolsuper and rolname in ('elicit_owner', 'dbowner')")
    [ -z "$left" ] || { echo "  still a superuser: $left" >&2; exit 1; }
    echo "  elicit_owner and dbowner are no longer superusers"
}

# psql against the db service. The collation mismatch warning PostgreSQL prints on every
# connection is the condition being fixed, so its three lines are dropped; anything else,
# errors included, is shown, and a failing statement fails the script (pipefail).
db_psql() {
    docker compose exec -T db psql -U survey -d "$1" -v ON_ERROR_STOP=1 -X -q -At -c "$2" 2>&1 |
        { grep -vE '^(WARNING:  database ".*" has a collation version mismatch|DETAIL:  The database was created using collation version|HINT:  Rebuild all objects in this database)' || true; }
}

# Stop the stack and delete the database volume, if there is one.
drop_volume() {
    echo "Stopping the stack"
    docker compose down
    echo "Deleting the volume $PGDATA_VOLUME"
    if docker volume inspect "$PGDATA_VOLUME" > /dev/null 2>&1; then
        docker volume rm "$PGDATA_VOLUME" > /dev/null
    fi
}

usage() {
    echo "Usage: $0 V2|V3" >&2
    echo "  V3  delete the volume $PGDATA_VOLUME (greenfield: next start initializes a fresh database)" >&2
    echo "  V2  fill $PGDATA_VOLUME from $V2_SOURCE and add the empty cluster directories (brownfield)" >&2
    exit 1
}

[ $# -eq 1 ] || usage
MODE=$(echo "$1" | tr '[:lower:]' '[:upper:]')

case "$MODE" in
    V3)
        drop_volume
        echo "Done. 'docker compose up -d' will initialize a fresh (greenfield) database."
        ;;
    V2)
        [ -d "$V2_SOURCE/pgdata" ] || { echo "$V2_SOURCE/pgdata not found" >&2; exit 1; }
        drop_volume
        echo "Filling $PGDATA_VOLUME with a copy of $V2_SOURCE and the empty cluster directories git does not keep"
        docker compose run --rm --no-deps -T --user root --entrypoint sh \
            -v "$PWD/$V2_SOURCE:/v2:ro" db -ec '
                data=/var/lib/postgresql/data
                cp -R /v2/. "$data"/
                for d in "$@"; do mkdir -p "$data/pgdata/$d"; done
                chown -R postgres:postgres "$data"
                chmod -R u=rwX,go= "$data/pgdata"
            ' sh "${EMPTY_CLUSTER_DIRS[@]}"
        echo "Matching the copy's collation version to the database image"
        docker compose up -d --wait db
        refresh_collation_versions
        echo "Adding Author's database, which the V2 cluster predates"
        create_author_database
        echo "Narrowing the schema owner, which the V2 cluster created as a superuser"
        narrow_owner_roles
        docker compose stop db
        echo "Done. 'docker compose up -d' will run the V2 -> V3 upgrade (brownfield) path."
        ;;
    *)
        usage
        ;;
esac
