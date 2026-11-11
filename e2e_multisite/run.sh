#!/bin/bash
# One complete run: wipe both sites, start them, run the journey.
# The journey installs the one Household Survey (a single survey key across both revisions and
# both sites), so it needs clean databases every time.
set -euo pipefail
cd "$(dirname "$0")"
./reset.sh all
./up.sh all
mvn -DskipTests=false test "$@"
