#!/bin/bash
#
# Lay out each site's translations mount from ../elicit-i18n, the platform's own translation
# source (see its README). This is the "i18n files" half of the journey's distribution step: a
# definition file carries the survey's content translations, but a site only serves a language
# whose application texts it also has (Survey UC-009 BR-009), and those arrive as files an
# operator drops into the mount -- not in the .elicit file.
#
#   i18n/usa/{survey,admin,author}/   empty: the master ships English only and hides the selector
#   i18n/mexico/{survey,admin}/       translations_es_419.properties
#   i18n/arabia/{survey,admin}/       translations_ar.properties + i18n-config.json
#
# Regenerated on every ./up.sh so the mounts never drift from ../elicit-i18n. The copies are
# gitignored; the directories are kept by .gitkeep.
set -euo pipefail
cd "$(dirname "$0")"
SRC=../elicit-i18n
[ -d "$SRC" ] || { echo "$SRC is missing: this suite needs the umbrella checkout" >&2; exit 1; }

# USA: English only. Empty app directories, so i18n.file.system.path is still exercised.
for app in survey admin author; do
    mkdir -p "i18n/usa/$app"
    rm -f "i18n/usa/$app"/translations*.properties
done
rm -f i18n/usa/i18n-config.json

copy_language() { # site file-tag
    local site=$1 tag=$2
    for app in survey admin; do
        mkdir -p "i18n/$site/$app"
        rm -f "i18n/$site/$app"/translations*.properties
        cp "$SRC/$app/translations_$tag.properties" "i18n/$site/$app/translations_$tag.properties"
    done
    echo "  $site: $app translations_$tag.properties"
}

copy_language mexico es_419
copy_language arabia ar
# Arabic is in the applications' own right-to-left list, so this only records the intent; any
# language that is not would need it (see elicit-i18n/README.md).
cp "$SRC/i18n-config.json" i18n/arabia/i18n-config.json

echo "Mounts laid out: i18n/usa (English only), i18n/mexico (es-419), i18n/arabia (ar)."
