#!/bin/bash
#
# Checks that the default brand is one brand: elicit-brand/ here is the source, and every copy of
# it says the same thing (umbrella NFR-015).
#
# The default brand exists in five places. elicit-brand/ is what docker-compose.yml mounts. Survey,
# Admin and Author each package a copy at src/main/resources/META-INF/brand/, which is what an
# application shows when a site mounts nothing. And docs/manual/elicit-brand.sty repeats the
# palette for the manuals. Nothing at runtime notices when they disagree: the copies had drifted to
# two logos and three sets of colors before this check existed. Two things are checked:
#
#   1. Each module's packaged brand holds the same files as elicit-brand/, with the same content.
#      A stylesheet in a module starts with that module's license header, which is not part of the
#      comparison. The SVG masters in images/source/ are not packaged: no application reads them,
#      and a module's build would stamp a license header into each.
#   2. Every \definecolor{elicit…} in docs/manual/elicit-brand.sty names a color of
#      elicit-brand/colors/brand-colors.css and carries its value: elicitInkDark is
#      --elicit-ink-dark.
#   3. Every logo file elicit-brand/brand-config.json names (logos.horizontal, logos.icon and
#      favicon) is under elicit-brand/images/, and the brand names its icon, because it brings its
#      own favicon (umbrella UC-014 BR-008, BR-009).
#
#   ./checkBrand.sh           check, exit non-zero on a difference
#   ./checkBrand.sh --sync    copy elicit-brand/ into the modules (keeping their license headers),
#                             then check. The .sty is edited by hand.
#
# Run from the umbrella root; buildDockerImages.sh runs it before any image is built. Cheap enough
# to run unconditionally: it reads files and starts no JVM.
set -uo pipefail
cd "$(dirname "$0")"

MODULES=(Survey Admin Author)       # the clones that package the brand
BRAND=elicit-brand
PACKAGED=src/main/resources/META-INF/brand
STY=docs/manual/elicit-brand.sty
COLORS=$BRAND/colors/brand-colors.css
status=0
sync=0
[ "${1:-}" = "--sync" ] && sync=1

all_files() {  # directory -> its files, relative, sorted
    (cd "$1" && find . -type f ! -name .DS_Store | sed 's|^\./||' | sort)
}

brand_files() {  # directory -> the files of it that a module packages
    all_files "$1" | grep -v '^images/source/'
}

license_lines() {  # file -> how many lines its leading license header takes, the blank after it included
    awk 'NR == 1 && $0 != "/*-" { print 0; found = 1; exit }
         / \*\/$/ { last = NR; getline next_line; print (next_line == "" ? last + 1 : last); found = 1; exit }
         END { if (!found) print 0 }' "$1"
}

without_license() {  # file -> its content after the license header
    tail -n +"$(( $(license_lines "$1") + 1 ))" "$1"
}

for m in "${MODULES[@]}"; do
    if [ ! -d "$m" ]; then
        # Never skipped: a missing clone is exactly when the copies would drift unnoticed.
        echo "ERROR: $m is not cloned, so its packaged brand cannot be checked. Run ./cloneAllProjects.sh" >&2
        status=1
        continue
    fi
    target=$m/$PACKAGED

    if [ "$sync" -eq 1 ]; then
        mkdir -p "$target"
        # Files the brand no longer has.
        all_files "$target" | while IFS= read -r f; do
            brand_files "$BRAND" | grep -qxF "$f" || rm "$target/$f"
        done
        brand_files "$BRAND" | while IFS= read -r f; do
            mkdir -p "$(dirname "$target/$f")"
            if [ "${f##*.}" = css ] && [ -f "$target/$f" ] && [ "$(license_lines "$target/$f")" -gt 0 ]; then
                header=$(head -n "$(license_lines "$target/$f")" "$target/$f"; echo x)
                printf '%s' "${header%x}" > "$target/$f.new"
                cat "$BRAND/$f" >> "$target/$f.new"
                mv "$target/$f.new" "$target/$f"
            else
                cp "$BRAND/$f" "$target/$f"
            fi
        done
        find "$target" -type d -empty -delete
    fi

    # 1. The same files, with the same content.
    if [ "$(brand_files "$BRAND")" != "$(brand_files "$target")" ]; then
        echo "ERROR: $target/ does not hold the same files as $BRAND/:" >&2
        diff <(brand_files "$BRAND") <(brand_files "$target") | grep '^[<>]' \
            | sed -e "s|^< |         only in $BRAND/: |" -e "s|^> |         only in $m: |" >&2
        status=1
    fi
    while IFS= read -r f; do
        [ -f "$target/$f" ] || continue
        if [ "${f##*.}" = css ]; then
            without_license "$target/$f" | cmp -s - "$BRAND/$f" && continue
        else
            cmp -s "$target/$f" "$BRAND/$f" && continue
        fi
        echo "ERROR: $target/$f differs from $BRAND/$f." >&2
        status=1
    done < <(brand_files "$BRAND")
done

# 2. The manuals' palette is the brand's palette.
css_color() {  # elicitInkDark -> the value of --elicit-ink-dark, upper case, or nothing
    local name
    name=$(printf '%s' "${1#elicit}" | sed 's/\([A-Z]\)/-\1/g' | tr '[:upper:]' '[:lower:]')
    grep -E "^[[:space:]]*--elicit${name}:" "$COLORS" | head -1 \
        | sed -E 's/.*#([0-9A-Fa-f]{6}).*/\1/' | tr '[:lower:]' '[:upper:]'
}

defined=0
while read -r name value; do
    defined=$((defined + 1))
    expected=$(css_color "$name")
    if [ -z "$expected" ]; then
        echo "ERROR: $STY defines $name, which is not a color of $COLORS." >&2
        status=1
    elif [ "$expected" != "$(printf '%s' "$value" | tr '[:lower:]' '[:upper:]')" ]; then
        echo "ERROR: $STY has $name as $value, but $COLORS has it as $expected." >&2
        status=1
    fi
done < <(sed -nE 's/^\\definecolor\{(elicit[A-Za-z]+)\}\{HTML\}\{([0-9A-Fa-f]{6})\}.*/\1 \2/p' "$STY")
if [ "$defined" -eq 0 ]; then
    echo "ERROR: no \\definecolor{elicit…} found in $STY; the palette check read nothing." >&2
    status=1
fi

# 3. The logo files the configuration names exist, and the icon is named beside the favicon.
logos=$(python3 - "$BRAND/brand-config.json" <<'PY'
import json, sys
config = json.load(open(sys.argv[1]))
logos = config.get("logos") or {}
for key, value in (("logos.horizontal", logos.get("horizontal")), ("logos.icon", logos.get("icon")),
                   ("favicon", config.get("favicon"))):
    print(key, value or "")
PY
) || { echo "ERROR: $BRAND/brand-config.json could not be read as JSON." >&2; status=1; }
while read -r key file; do
    if [ -z "$file" ]; then
        echo "ERROR: $BRAND/brand-config.json names no $key; the default brand names all three logo files." >&2
        status=1
    elif [ ! -f "$BRAND/images/$file" ]; then
        echo "ERROR: $BRAND/brand-config.json names $key $file, which is not in $BRAND/images/." >&2
        status=1
    fi
done <<< "$logos"

if [ "$status" -eq 0 ]; then
    echo "Survey, Admin and Author package $BRAND/; the manuals' $defined colors match it; its three logo files are named and present"
else
    echo "       $BRAND/ is the source. Run ./checkBrand.sh --sync to copy it into the modules." >&2
fi
exit "$status"
