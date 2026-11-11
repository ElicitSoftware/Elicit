#!/bin/bash
# Full platform test, in order: build the images, a greenfield run, a brownfield run,
# and both multi-site suites. Stops at the first failing stage, prints which one and
# its log, and exits non-zero. One log per stage under test-logs/.
#
#   ./fullTest.sh              everything
#   SKIP_BUILD=1 ./fullTest.sh reuse the images already built
#
# Greenfield: resetDatabase.sh V3, then e2e-tests, which creates a department and
# imports FHHS/family-history-survey.elicit. Brownfield: resetDatabase.sh V2, Survey
# restarted as deploy.sh does, then e2e-tests against the upgraded V2 database. Each
# prints the surveys, their report schemas and the Family History Survey's dim_step
# count to test-logs/db-*.log. The multi-site suites bring up and take down stacks of
# their own on the same ports, so this script stops the umbrella stack first.
set -uo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
LOGS="$ROOT/test-logs"
mkdir -p "$LOGS"
rm -f "$LOGS"/*.log

stage() { echo "=== $(date +%H:%M:%S) $1"; }
fail()  { echo "=== FAILED: $1 (see $2)"; exit 1; }

# Start the stack and wait for the named services (all of them when none are named).
wait_stack() {
    local name=$1; shift
    docker compose up -d > "$LOGS/up-$name.log" 2>&1 || return 1
    docker compose up -d --wait "$@" >> "$LOGS/up-$name.log" 2>&1 || return 1
    ./status.sh > "$LOGS/status-$name.log" 2>&1
}

db_report() {
    docker exec elicit-db-1 psql -U survey -d survey -At \
        -c "select name, report_schema from survey.surveys order by 1;" \
        -c "select count(*) from report_family_history_survey.dim_step;" \
        -c "select count(*) from survey.respondents;" > "$LOGS/db-$1.log" 2>&1
}

if [ -z "${SKIP_BUILD:-}" ]; then
    stage "build images"
    ./buildDockerImages.sh > "$LOGS/build.log" 2>&1 || fail build "$LOGS/build.log"
fi

stage "greenfield: reset V3 and start"
./resetDatabase.sh V3 > "$LOGS/reset-green.log" 2>&1 || fail "reset V3" "$LOGS/reset-green.log"
# FHHS is not ready on a fresh database until the survey is imported, so wait only
# for the services the import needs.
wait_stack green db keycloak survey admin author author-survey pedigree mailpit sftpServer \
    || fail "greenfield up" "$LOGS/up-green.log"
stage "greenfield: e2e-tests"
(cd e2e-tests && mvn -B -DskipTests=false test > "$LOGS/e2e-green.log" 2>&1) \
    || fail "greenfield e2e" "$LOGS/e2e-green.log"
./status.sh > "$LOGS/status-green-after.log" 2>&1
db_report green

stage "brownfield: reset V2 and start"
./resetDatabase.sh V2 > "$LOGS/reset-brown.log" 2>&1 || fail "reset V2" "$LOGS/reset-brown.log"
wait_stack brown || fail "brownfield up" "$LOGS/up-brown.log"
docker restart elicit-survey-1 > /dev/null   # deploy.sh's second step, for a pre-change database
wait_stack brown2 || fail "brownfield restart" "$LOGS/up-brown2.log"
stage "brownfield: e2e-tests"
(cd e2e-tests && mvn -B -DskipTests=false test > "$LOGS/e2e-brown.log" 2>&1) \
    || fail "brownfield e2e" "$LOGS/e2e-brown.log"
./status.sh > "$LOGS/status-brown-after.log" 2>&1
db_report brown
docker compose down > /dev/null 2>&1

stage "e2e_multisite"
(cd e2e_multisite && ./run.sh -B > "$LOGS/multisite.log" 2>&1); rc=$?
(cd e2e_multisite && ./down.sh > /dev/null 2>&1)
[ $rc -eq 0 ] || fail "e2e_multisite" "$LOGS/multisite.log"

stage "e2e_multisite_multilingual"
(cd e2e_multisite_multilingual && ./run.sh -B > "$LOGS/multilingual.log" 2>&1); rc=$?
(cd e2e_multisite_multilingual && ./down.sh > /dev/null 2>&1)
[ $rc -eq 0 ] || fail "e2e_multisite_multilingual" "$LOGS/multilingual.log"

stage "all stages passed"
