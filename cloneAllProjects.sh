#!/bin/bash
#
# Clone the five module repositories into this directory, beside the compose file, so the build
# and test scripts run from the root of the repository. All five are public, so they are cloned
# over HTTPS; pass --ssh to clone over SSH instead (for pushing with an SSH key).
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
