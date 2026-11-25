#!/bin/bash
#
# Checks that every repository says the same thing about who holds the copyright and under which
# license (C-014): Elicit Software LLC, PolyForm Noncommercial 1.0.0 (Pedigree: MIT), with the
# University of Michigan's contribution stated in the LICENSE file and nowhere else.
#
# Seven repositories state it in five kinds of place, and before 3.0.0 they named five different
# holders between them. Nothing at build time or runtime notices such a drift, so it is checked:
#
#   1. LICENSE.MD in Survey, Admin, FHHS, Author and elicit_db is this repository's LICENSE.MD,
#      byte for byte. Pedigree's LICENSE.md (MIT) names Elicit Software LLC and carries the
#      contribution sentence.
#   2. The header plugin of each Java module writes Elicit Software LLC (pom.xml,
#      organizationName), and its license template (src/license/polyform/license.txt) is
#      LICENSE.MD without the contribution notice.
#   3. Every generated file header (the line between the two %% of a ***LICENSE_START*** block)
#      reads "Copyright (C) 2025 - <year> Elicit Software LLC". The Flyway migrations are included:
#      a header edited by hand there changes a checksum.
#   4. No tracked file but a LICENSE file names the University as a copyright holder, and no tracked
#      file names the cancer center the headers once carried.
#
# Survey, Admin, FHHS and Author must be cloned; Pedigree and elicit_db are checked when they are
# (the umbrella's CI clones only the four). Run from the umbrella root; buildDockerImages.sh runs
# it after checkBrand.sh. It reads files and starts no JVM.
set -uo pipefail
cd "$(dirname "$0")"

JAVA_MODULES=(Survey Admin FHHS Author)
HOLDER="Elicit Software LLC"
NOTICE="Required Notice: Copyright © 2025-2026 $HOLDER (https://elicit.software)"
PORTIONS="Portions contributed by The Regents of the University of Michigan."
HEADER_LINE='Copyright \(C\) 2025 - 20[0-9][0-9] Elicit Software LLC$'
FORMER_NAME='rog''el'    # split so that this script does not match its own check
status=0

fail() {
    echo "ERROR: $1" >&2
    shift
    for line in "$@"; do echo "       $line" >&2; done
    status=1
}

# 1. The license files.
[ "$(sed -n 1p LICENSE.MD)" = "$NOTICE" ] \
    || fail "LICENSE.MD does not open with: $NOTICE"
[ "$(sed -n 3p LICENSE.MD)" = "Required Notice: $PORTIONS" ] \
    || fail "LICENSE.MD line 3 is not: Required Notice: $PORTIONS"

for m in "${JAVA_MODULES[@]}"; do
    if [ ! -d "$m" ]; then
        fail "$m is not cloned, so its license statements cannot be checked. Run ./cloneAllProjects.sh"
    fi
done
[ "$status" -eq 0 ] || exit 1

polyform=("${JAVA_MODULES[@]}")
[ -d elicit_db ] && polyform+=(elicit_db)
for m in "${polyform[@]}"; do
    cmp -s LICENSE.MD "$m/LICENSE.MD" \
        || fail "$m/LICENSE.MD differs from the umbrella's LICENSE.MD." "Copy LICENSE.MD into $m."
done
[ ! -e elicit_db/LICENSE ] || fail "elicit_db/LICENSE (Apache-2.0) is back beside LICENSE.MD."

if [ -d Pedigree ]; then
    grep -qF "Copyright © 2025-2026 $HOLDER" Pedigree/LICENSE.md \
        || fail "Pedigree/LICENSE.md does not name $HOLDER as copyright holder."
    grep -qxF "$PORTIONS" Pedigree/LICENSE.md \
        || fail "Pedigree/LICENSE.md lacks the line: $PORTIONS"
fi

# 2. What the header plugin will write into a new file.
for m in "${JAVA_MODULES[@]}"; do
    grep -qF "<organizationName>$HOLDER</organizationName>" "$m/pom.xml" \
        || fail "$m/pom.xml: the license plugin's organizationName is not $HOLDER."
    grep -qF "<inceptionYear>2025</inceptionYear>" "$m/pom.xml" \
        || fail "$m/pom.xml: the license plugin's inceptionYear is not 2025." \
                "A new file's header would not read \"2025 - <year>\"."
    template="$m/src/license/polyform/license.txt"
    if ! diff -q <(grep -vxF "Required Notice: $PORTIONS" LICENSE.MD | cat -s) <(cat -s "$template") >/dev/null; then
        fail "$template is not LICENSE.MD without the contribution notice."
    fi
done

# 3. The headers that exist.
for m in "${JAVA_MODULES[@]}"; do
    wrong=$(git -C "$m" grep -InE 'Copyright \(C\)' -- '*.java' '*.css' '*.sql' '*.html' '*.js' \
            | grep -vE "$HEADER_LINE" | head -5)
    [ -z "$wrong" ] || fail "$m: copyright lines that are not \"Copyright (C) 2025 - <year> $HOLDER\":" \
                            "$wrong" "Let the build write a header; never paste or edit one."
done

# 4. Nothing else names the University as holder, and nothing names the cancer center.
repos=(. "${JAVA_MODULES[@]}")
[ -d Pedigree ] && repos+=(Pedigree)
[ -d elicit_db ] && repos+=(elicit_db)
for r in "${repos[@]}"; do
    wrong=$(git -C "$r" grep -InE 'Copyright.*(Regents|University of Michigan)' -- ':!LICENSE*' ':!checkLicense.sh' | head -5)
    [ -z "$wrong" ] || fail "$r: the University is named as a copyright holder outside the LICENSE file:" "$wrong"
    wrong=$(git -C "$r" grep -Iil "$FORMER_NAME" | head -5)
    [ -z "$wrong" ] || fail "$r: files naming the cancer center of the former headers:" "$wrong"
done

if [ "$status" -eq 0 ]; then
    echo "Copyright $HOLDER in ${repos[*]}; the University is credited in the LICENSE files only"
fi
exit "$status"
