#!/bin/bash
#
# Checks that Survey and Admin agree on which languages Elicit carries.
#
# Each module packages its translated bundles from its own i18n/ directory onto the classpath and
# declares them in i18n.bundled.locales. Nothing at runtime can reconcile the two: classpath
# resources cannot be enumerated, so a bundle that is shipped but not declared is simply never
# offered, and a tag declared with no bundle offers a language that renders entirely in English.
# Both are silent.
#
# The cross-module half matters more. A language one application has and another lacks cannot be
# served, because Survey renders content only in a language its own chrome has (Survey UC-009
# BR-009).
#
# The authoring tool is the third party to that invariant, and the one most likely to break it:
# it declares which languages a survey's *content* may be published in, and content published in
# a language no site can render is exactly the failure this guards against. Author is a separate
# private repository and is not cloned here, so this script cannot check it. Author's own build
# runs the full three-way check -- it sits inside an umbrella checkout and can see Survey and
# Admin -- and that run is the authoritative one.
#
# So this is an invariant the build holds rather than a convention. Run from the umbrella root;
# buildDockerImages.sh runs it before any image is built. Cheap enough to run unconditionally:
# it reads files and starts no JVM.
set -uo pipefail
cd "$(dirname "$0")"

MODULES=(Survey Admin)
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

# 2. The modules must ship the same languages.
reference=${shipped[0]}
for i in "${!MODULES[@]}"; do
    [ "$i" -eq 0 ] && continue
    if [ "${shipped[$i]}" != "$reference" ]; then
        echo "ERROR: ${MODULES[$i]} ships [${shipped[$i]}] but ${MODULES[0]} ships [$reference]." >&2
        echo "       A language one application has and another lacks cannot be served: Survey" >&2
        echo "       renders content only in a language its own chrome has." >&2
        status=1
    fi
done

# 3. The authoring tool, when it happens to be checked out beside us. This never fails the build
# and deliberately does not touch $status. Author is a separate private repository that is not
# cloned here, so whatever is or is not sitting in ./Author -- a partial clone, a worktree
# mid-rebase, nothing at all -- must never be able to stop the public build. Author's own build
# enforces the three-way invariant; this is a courtesy heads-up for a developer who has both.
if [ -d Author ]; then
    author_shipped=$(tags_from_files Author)
    author_content=$(tags_from_property Author author.content.languages)
    if [ "$author_shipped" != "$reference" ] || [ "$author_content" != "$reference" ]; then
        echo "WARNING: the Author checkout beside this one ships [$author_shipped] and publishes" >&2
        echo "         content in [$author_content], while Survey and Admin ship [$reference]." >&2
        echo "         Author's own build is what enforces this; it is only reported here." >&2
    fi
fi

if [ "$status" -eq 0 ]; then
    if [ -d Author ]; then
        echo "Languages agree across Survey, Admin and Author: [$reference]"
    else
        echo "Languages agree across Survey and Admin: [$reference]"
    fi
fi
exit $status
