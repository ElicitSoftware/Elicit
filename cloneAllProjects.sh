#!/bin/bash
#
# Clone the five module repositories and the elicit_db image's source into this directory, beside
# the compose file, so the build and test scripts run from the root of the repository. All six are
# public, so they are cloned over HTTPS; pass --ssh to clone over SSH instead (for pushing with an
# SSH key). PostgreSQL is cloned as elicit_db/, after the image it builds: a PostgreSQL/ beside
# postgresql/ (where the stack kept its data before it moved to a named volume, and still there
# in older checkouts) is the same directory on a case-insensitive filesystem.
#
#   ./cloneAllProjects.sh          https://github.com/ElicitSoftware/<module>.git
#   ./cloneAllProjects.sh --ssh    git@github.com:ElicitSoftware/<module>.git

set -euo pipefail
cd "$(dirname "$0")"

base="https://github.com/ElicitSoftware"
if [[ "${1:-}" == "--ssh" ]]; then
  base="git@github.com:ElicitSoftware"
fi

for module in Admin Author FHHS Pedigree Survey; do
  git clone "$base/$module.git" "$module"
done
git clone "$base/PostgreSQL.git" elicit_db
