#!/bin/bash
#
# Build every Elicit module image in parallel.
#
#   ./buildDockerImages.sh            build all modules
#   ./buildDockerImages.sh Admin FHHS build only the named modules
#
# Each module's buildDockerImage.sh runs in the background with its output in
# build-logs/<Module>.log. The script waits for all of them, prints a summary, and
# exits non-zero if any build failed.
#
# Nothing forces an order: no image is built FROM another, no pom depends on
# another module's artifact, each module has its own target/ and node_modules/,
# and the Quarkus test ports are distinct (Survey 8089, Admin 8090, FHHS 8091,
# Author 8092). The only shared resource is ~/.m2, which Maven 3.9 handles
# concurrently; on a cold cache a rare download collision is fixed by rerunning.
#
# PREMM5 is not cloned by cloneAllProjects.sh and is commented out of
# docker-compose.yml; add it to MODULES if you restore that module. The
# postgresql/ directory holds only the local PGDATA volume -- there is no build
# script there. The elicitsoftware/elicit_db image is pulled, not built.

set -u
cd "$(dirname "$0")"

ALL_MODULES=(Survey FHHS Pedigree Admin Author)
if [ $# -gt 0 ]; then MODULES=("$@"); else MODULES=("${ALL_MODULES[@]}"); fi
LOG_DIR=build-logs

for m in "${MODULES[@]}"; do
    if [ ! -x "$m/buildDockerImage.sh" ]; then
        echo "No executable $m/buildDockerImage.sh" >&2
        exit 2
    fi
done

mkdir -p "$LOG_DIR"

# Two runs at once would run `mvn clean` under each other, collide on the test
# ports and overwrite each other's logs, so refuse to start while another run holds
# the lock. A stale lock (its PID no longer alive) is taken over.
LOCK_DIR="$LOG_DIR/.lock"
if ! mkdir "$LOCK_DIR" 2>/dev/null; then
    other=$(cat "$LOCK_DIR/pid" 2>/dev/null)
    if [ -n "$other" ] && kill -0 "$other" 2>/dev/null; then
        echo "Another buildDockerImages.sh (pid $other) is still running; wait for it to finish." >&2
        exit 3
    fi
    rm -rf "$LOCK_DIR" && mkdir "$LOCK_DIR"
fi
echo $$ > "$LOCK_DIR/pid"
trap 'rm -rf "$LOCK_DIR"' EXIT

for m in "${MODULES[@]}"; do rm -f "$LOG_DIR/$m.log" "$LOG_DIR/$m.status"; done

echo "Building ${MODULES[*]} in parallel; logs in $LOG_DIR/"
START=$(date +%s)
for m in "${MODULES[@]}"; do
    (
        t0=$(date +%s)
        (cd "$m" && ./buildDockerImage.sh)
        rc=$?
        echo "$rc $(( $(date +%s) - t0 ))" > "$LOG_DIR/$m.status"
    ) > "$LOG_DIR/$m.log" 2>&1 &
done

# Report each build as it finishes, in the order they actually finish.
pending=("${MODULES[@]}")
while [ ${#pending[@]} -gt 0 ]; do
    sleep 5
    still=()
    for m in "${pending[@]}"; do
        if [ -f "$LOG_DIR/$m.status" ]; then
            read -r rc secs < "$LOG_DIR/$m.status"
            if [ "$rc" -eq 0 ]; then
                printf '  %-9s ok      %2dm%02ds\n' "$m" $((secs / 60)) $((secs % 60))
            else
                printf '  %-9s FAILED  %2dm%02ds  (exit %s, see %s/%s.log)\n' "$m" $((secs / 60)) $((secs % 60)) "$rc" "$LOG_DIR" "$m"
            fi
        else
            still+=("$m")
        fi
    done
    pending=("${still[@]+"${still[@]}"}")
done
wait

TOTAL=$(( $(date +%s) - START ))
failed=()
for m in "${MODULES[@]}"; do
    read -r rc _ < "$LOG_DIR/$m.status"
    [ "$rc" -eq 0 ] || failed+=("$m")
done

echo
if [ ${#failed[@]} -eq 0 ]; then
    printf 'All %d images built in %dm%02ds\n' "${#MODULES[@]}" $((TOTAL / 60)) $((TOTAL % 60))
    exit 0
fi

printf 'FAILED: %s (%dm%02ds)\n' "${failed[*]}" $((TOTAL / 60)) $((TOTAL % 60))
for m in "${failed[@]}"; do
    echo
    echo "--- $LOG_DIR/$m.log (errors and last lines) ---"
    grep -n 'ERROR\|FAILURE\|<<< FAIL\|Tests run:.*Failures: [1-9]\|Tests run:.*Errors: [1-9]' "$LOG_DIR/$m.log" | head -20
    tail -n 15 "$LOG_DIR/$m.log"
done
exit 1
