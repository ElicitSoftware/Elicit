#!/bin/bash
#
# One complete run, recorded: wipe all three sites, start them, run the journey with the
# recording harness on, and assemble the film.
#
#   ./record.sh                 reset, start, record, assemble
#   ./record.sh --speed 1.0     the same at life size, rather than the default quarter-slower
#   ./record.sh --no-reset      record against stacks that are already up and clean
#
# The journey installs the one Census Household Survey, so it needs clean databases every time --
# exactly as ./run.sh does, and for the same reason. Anything else on the command line goes to
# make-recording.py (--speed, --card-seconds, --no-cards, --out; see ./make-recording.py --help).
#
# A recorded run is slower than a plain one: Chromium is launched with a slowMo so the synthetic
# pointer has time to arrive before each click lands (Recording.SLOW_MO_DEFAULT), and that makes a
# Vaadin form legible rather than instantaneous. Budget half an hour rather than fifteen minutes.
set -euo pipefail
cd "$(dirname "$0")"

reset=1
assembly=()
for argument in "$@"; do
    if [ "$argument" = "--no-reset" ]; then
        reset=0
    else
        assembly+=("$argument")
    fi
done

if [ "$reset" = 1 ]; then
    ./reset.sh all
    ./up.sh all
fi

mvn -DskipTests=false -De2e.record=true test
./make-recording.py "${assembly[@]+"${assembly[@]}"}"
