#!/bin/bash
#
# Checks the languages Elicit offers: the Survey and Admin images carry English only, and the
# curated translations in elicit-i18n/ give every application the same set of languages.
#
# The images carry English only (Elicit#62). Every other language is a
# translations_<tag>.properties file a site mounts at <i18n.file.system.path>/<app>/; elicit-i18n/
# here is the curated set ElicitSoftware maintains beside each release, mounted by
# docker-compose.yml. Three things go wrong silently and are checked here:
#
#   1. A translated bundle reaches a module's classpath again (src/main/resources/vaadin-i18n or a
#      leftover i18n/translations_*.properties the pom might package). The image would then carry a
#      language the site did not mount, and a mounted file would only overlay it.
#   2. A module still sets i18n.bundled.locales. That property no longer exists; a site that kept it
#      would believe it had narrowed its languages when nothing reads it. i18n.offered.locales is
#      the setting now.
#   3. elicit-i18n/survey, elicit-i18n/admin and elicit-i18n/author hold different languages. A
#      language one application has and another lacks cannot be served, because Survey renders
#      content only in a language its own chrome has (Survey UC-009 BR-009).
#
# The authoring tool reads the same mount (Author#79): its own compose file mounts ../elicit-i18n
# and it reads <mount>/author/, so elicit-i18n/author/ is content of this repository and is held
# to the same set as the other two. Author is also the third party to the invariant: it declares
# which languages a survey's *content* may be published in (author.content.languages). Author
# itself is a separate private repository that is not cloned here, so when a checkout happens to
# sit beside this one, what it declares and what it packages are only reported, never failed on.
#
# Run from the umbrella root; buildDockerImages.sh runs it before any image is built. Cheap enough
# to run unconditionally: it reads files and starts no JVM.
set -uo pipefail
cd "$(dirname "$0")"

MODULES=(Survey Admin)              # the clones whose images are checked (1 and 2)
APPS=(survey admin author)          # the sub-directories of the mount that must agree (3)
MOUNT=elicit-i18n
status=0

# English is not a translated bundle: it is authored in each image at
# src/main/resources/vaadin-i18n/translations.properties, so there is no translations_en.properties
# to find. It is implicit everywhere and added here rather than discovered.
tags_in() {  # directory -> comma-separated sorted tags, en included
    local dir=$1
    local tag f
    local list=en
    for f in "$dir"/translations_*.properties; do
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

app_dir() {  # module -> its sub-directory of the mount (i18n.app.name)
    echo "$1" | tr '[:upper:]' '[:lower:]'
}

app_name() {  # sub-directory of the mount -> the application's name
    echo "$(echo "${1:0:1}" | tr '[:lower:]' '[:upper:]')${1:1}"
}

for m in "${MODULES[@]}"; do
    if [ ! -d "$m" ]; then
        # Never skipped: a missing clone is exactly when the invariant would lapse unnoticed.
        echo "ERROR: $m is not cloned, so its languages cannot be checked. Run ./cloneAllProjects.sh" >&2
        status=1
        continue
    fi

    # 1. The image carries English only.
    for f in "$m"/src/main/resources/vaadin-i18n/translations_*.properties "$m"/i18n/translations_*.properties; do
        [ -e "$f" ] || continue
        echo "ERROR: $f would put a translated bundle in the $m image." >&2
        echo "       The images carry English only; move it to $MOUNT/$(app_dir "$m")/." >&2
        status=1
    done

    # 2. The retired property.
    if [ "$(tags_from_property "$m" i18n.bundled.locales)" != UNSET ]; then
        echo "ERROR: $m still sets i18n.bundled.locales, which nothing reads any more." >&2
        echo "       Use i18n.offered.locales to offer fewer languages than are mounted." >&2
        status=1
    fi
done
[ "$status" -eq 0 ] || exit 1

# 3. The curated set gives every application the same languages. author/ is checked like the other
#    two: the directory is content of this repository, whatever state the Author checkout is in.
reference=
reference_app=
for app in "${APPS[@]}"; do
    if [ ! -d "$MOUNT/$app" ]; then
        echo "ERROR: $MOUNT/$app/ is missing; $(app_name "$app") would offer English only." >&2
        status=1
        continue
    fi
    tags=$(tags_in "$MOUNT/$app")
    if [ -z "$reference" ]; then
        reference=$tags
        reference_app=$app
        continue
    fi
    if [ "$tags" != "$reference" ]; then
        echo "ERROR: $MOUNT/$app/ holds [$tags] but $MOUNT/$reference_app/ holds [$reference]." >&2
        echo "       A language one application has and another lacks cannot be served: Survey" >&2
        echo "       renders content only in a language its own chrome has." >&2
        status=1
    fi
done
[ "$status" -eq 0 ] || exit 1

# The authoring tool, when it happens to be checked out beside us. This never fails the build
# and deliberately does not touch $status. Author is a separate private repository that is not
# cloned here, so whatever is or is not sitting in ./Author -- a partial clone, a worktree
# mid-rebase, nothing at all -- must never be able to stop the public build. Since Author#79 its
# image carries English only too and it reads $MOUNT/author/ from the same mount, so a checkout
# that still packages a language or still sets i18n.bundled.locales is behind the mount; its
# content languages are what matter to the invariant. Author's own build is what enforces all
# three; here they are only reported.
if [ -d Author ]; then
    for f in Author/src/main/resources/vaadin-i18n/translations_*.properties Author/i18n/translations_*.properties; do
        [ -e "$f" ] || continue
        echo "WARNING: $f would put a translated bundle in the Author image." >&2
        echo "         Author reads its languages from $MOUNT/author/ since Author#79." >&2
    done
    author_bundled=$(tags_from_property Author i18n.bundled.locales)
    if [ "$author_bundled" != UNSET ] && [ "$author_bundled" != MISSING-FILE ]; then
        echo "WARNING: the Author checkout beside this one still sets i18n.bundled.locales [$author_bundled]," >&2
        echo "         which Author stopped reading when it moved to the mount (Author#79)." >&2
    fi
    author_content=$(tags_from_property Author author.content.languages)
    if [ "$author_content" != "$reference" ]; then
        echo "WARNING: the Author checkout beside this one publishes content in [$author_content]," >&2
        echo "         while $MOUNT/ gives Survey, Admin and Author [$reference]." >&2
    fi
fi

if [ "$status" -eq 0 ]; then
    echo "Images carry English only; $MOUNT/ gives Survey, Admin and Author [$reference]"
fi
exit $status
