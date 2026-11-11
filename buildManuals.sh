#!/bin/bash
#
# Typeset every Elicit manual, fresh, in one run (#76).
#
#   ./buildManuals.sh                    all four manuals
#   ./buildManuals.sh Admin Author       only the named targets
#
# Targets, in the order they are built:
#
#   Platform            docs/manual/elicit-installation-manual.pdf (umbrella UC-002)
#   Admin               Admin/src/main/resources/manual/elicit-admin-manual.pdf (Admin UC-029)
#   Author              Author/src/main/resources/manual/elicit-author-manual.pdf (Author UC-039)
#   AuthorInstallation  Author/docs/manual/elicit-author-installation-manual.pdf (Author UC-051)
#
# Each manual is built by its own docs/manual/build-manual.sh, so where it is written, how it
# is stamped (the version its sources carry and the current UTC time) and which TeX Live image
# typesets it stay that script's business. This script adds two things:
#
#   - A clean start. latexmk keeps its auxiliary files in each manual's build/ directory and
#     can report a manual "up-to-date" after its sources changed; that directory is removed
#     before every typeset.
#   - The configuration gate for both installation manuals. The platform manual runs
#     docs/manual/check-properties.sh first, as buildDockerImages.sh's Manual target does;
#     Author's installation manual runs the same check from inside its own build-manual.sh.
#     SKIP_PROPERTY_CHECK=1 typesets both without it.
#
# The Admin and Author user manuals ship inside their images, and each module's
# buildDockerImage.sh reuses the PDF it finds there rather than typesetting again. So
#
#   ./buildManuals.sh && ./buildDockerImages.sh
#
# builds images that carry the manuals just typeset, stamped by this run.
#
# A target whose module is not cloned is skipped. A failed target does not stop the others;
# the run prints a summary and exits non-zero if any target failed. Docker must be running.

set -u
cd "$(dirname "$0")"

ALL_TARGETS=(Platform Admin Author AuthorInstallation)
if [ $# -gt 0 ]; then TARGETS=("$@"); else TARGETS=("${ALL_TARGETS[@]}"); fi

# The directory holding a target's manual sources and build-manual.sh.
manual_dir() {
    case "$1" in
        Platform)                  echo docs/manual ;;
        Admin)                     echo Admin/docs/manual ;;
        Author|AuthorInstallation) echo Author/docs/manual ;;
    esac
}

# The LaTeX source a target typesets; it is missing when the clone predates that manual.
manual_source() {
    case "$1" in
        Platform)           echo docs/manual/elicit-installation-manual.tex ;;
        Admin)              echo Admin/docs/manual/elicit-admin-manual.tex ;;
        Author)             echo Author/docs/manual/elicit-author-manual.tex ;;
        AuthorInstallation) echo Author/docs/manual/elicit-author-installation-manual.tex ;;
    esac
}

build_target() {
    local dir
    dir="$(manual_dir "$1")"
    rm -rf "${dir}/build"
    case "$1" in
        Platform)
            if [ "${SKIP_PROPERTY_CHECK:-0}" != 1 ]; then
                docs/manual/check-properties.sh || return 1
            fi
            docs/manual/build-manual.sh ;;
        Admin)
            "${dir}/build-manual.sh" ;;
        Author)
            MANUAL=author "${dir}/build-manual.sh" ;;
        AuthorInstallation)
            MANUAL=installation "${dir}/build-manual.sh" ;;
    esac
}

for t in "${TARGETS[@]}"; do
    if [ -z "$(manual_dir "$t")" ]; then
        echo "Unknown target '$t'; the targets are ${ALL_TARGETS[*]}." >&2
        exit 2
    fi
done

if ! docker info >/dev/null 2>&1; then
    echo "Docker is not running; every manual is typeset in a TeX Live container." >&2
    exit 1
fi

summary=()
failed=0
for t in "${TARGETS[@]}"; do
    if [ ! -x "$(manual_dir "$t")/build-manual.sh" ] || [ ! -f "$(manual_source "$t")" ]; then
        echo "== ${t}: skipped, $(manual_source "$t") is not in this checkout"
        summary+=("${t}: skipped (not cloned or no such manual)")
        continue
    fi
    echo "== ${t}"
    if build_target "$t"; then
        summary+=("${t}: ok")
    else
        summary+=("${t}: FAILED")
        failed=1
    fi
done

echo
echo "Manuals:"
for line in "${summary[@]}"; do echo "  ${line}"; done
exit "${failed}"
