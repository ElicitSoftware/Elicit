#!/bin/bash
#
# Checks that Survey, Admin and Author agree on which languages Elicit carries.
#
# Each module packages its translated bundles from its own i18n/ directory onto the classpath,
# declares them in i18n.bundled.locales, and Author separately declares which of them a survey's
# content may be published in (author.content.languages). Nothing at runtime can reconcile those:
# classpath resources cannot be enumerated, so a bundle that is shipped but not declared is simply
# never offered, and a tag declared with no bundle offers a language that renders entirely in
# English. Both are silent.
#
# The cross-module half matters more. A language Author has but Survey lacks lets an author publish
# survey content that no respondent could ever read, because Survey serves content only in a
# language its own chrome has (Survey UC-009 BR-009) -- which is the failure Author#40 exists to
# prevent, reintroduced from the other end.
#
# So this is an invariant the build holds rather than a convention. Run from the umbrella root;
# buildDockerImages.sh runs it before any image is built. Cheap enough to run unconditionally:
# it reads files and starts no JVM.
set -uo pipefail
cd "$(dirname "$0")"

MODULES=(Survey Admin Author)
status=0
# Indexed arrays, aligned with MODULES: /bin/bash on macOS is 3.2 and has no associative arrays.
shipped=()
declared=()

# English is not a translated bundle: it is authored at src/main/resources/vaadin-i18n/
# translations.properties, so there is no translations_en.properties to find. It is implicit
# everywhere and added here rather than discovered.
tags_from_files() {  # module -> comma-separated sorted tags, en included
    local module=$1
    local tag f
    local list=en
    for f in "$module"/i18n/translations_*.properties; do
        [ -e "$f" ] || continue
        tag=${f##*/translations_}
        tag=${tag%.properties}
        # Bundle files spell a tag with underscores (translations_es_419.properties); every tag in
        # a property, a database column and the runtime uses hyphens. Normalize or nothing matches.
        list="$list
${tag//_/-}"
    done
    printf '%s\n' "$list" | sort -u | paste -sd, -
}

tags_from_property() {  # module, property name -> comma-separated sorted tags
    # Separate statements on purpose: `local` expands all its arguments before it assigns any, so
    # `local module=$1 file="$module/..."` reads module while it is still unset -- which set -u
    # turns into an error rather than an empty path.
    local module=$1
    local prop=$2
    local file="$module/src/main/resources/application.properties"
    local value
    [ -f "$file" ] || { echo "MISSING-FILE"; return; }
    value=$(grep -E "^${prop//./\\.}=" "$file" | tail -1 | cut -d= -f2-)
    [ -n "$value" ] || { echo "UNSET"; return; }
    printf '%s\n' "${value//,/$'\n'}" | tr -d ' ' | grep -v '^$' | sort -u | paste -sd, -
}

for i in "${!MODULES[@]}"; do
    m=${MODULES[$i]}
    if [ ! -d "$m" ]; then
        # Never skipped: a missing clone is exactly when the invariant would lapse unnoticed.
        echo "ERROR: $m is not cloned, so its languages cannot be checked. Run ./cloneAllProjects.sh" >&2
        status=1
        continue
    fi
    shipped[$i]=$(tags_from_files "$m")
    declared[$i]=$(tags_from_property "$m" i18n.bundled.locales)
done
[ "$status" -eq 0 ] || exit 1

# 1. Each module's declaration must match the bundles it actually ships.
for i in "${!MODULES[@]}"; do
    m=${MODULES[$i]}
    if [ "${declared[$i]}" != "${shipped[$i]}" ]; then
        echo "ERROR: $m ships [${shipped[$i]}] but i18n.bundled.locales declares [${declared[$i]}]." >&2
        echo "       A bundle that is shipped but not declared is never offered; a tag declared" >&2
        echo "       with no bundle offers a language that renders entirely in English." >&2
        status=1
    fi
done

# 2. All three modules must ship the same languages.
reference=${shipped[0]}
for i in 1 2; do
    if [ "${shipped[$i]}" != "$reference" ]; then
        echo "ERROR: ${MODULES[$i]} ships [${shipped[$i]}] but ${MODULES[0]} ships [$reference]." >&2
        echo "       A language one application has and another lacks cannot be served: Survey" >&2
        echo "       renders content only in a language its own chrome has." >&2
        status=1
    fi
done

# 3. Author's publishable content languages must be that same set.
content=$(tags_from_property Author author.content.languages)
if [ "$content" != "$reference" ]; then
    echo "ERROR: Author's author.content.languages is [$content] but the applications ship [$reference]." >&2
    echo "       An author could publish survey content in a language no site can render." >&2
    status=1
fi

if [ "$status" -eq 0 ]; then
    echo "Languages agree across Survey, Admin and Author: [$reference]"
fi
exit $status
