#!/usr/bin/env bash
#
# Checks the installation manual's configuration reference against the code it documents
# (umbrella NFR-004, NFR-005; UC-002 A4).
#
# It answers three questions:
#   1. Which configuration keys does a module read that the manual does not document?
#   2. Which keys does the manual document that no module reads any more?
#   3. Which documented defaults disagree with the value in the module's own sources?
#
# NFR-004 sets the gate: every key a module reads through @ConfigProperty or a programmatic
# getConfig() lookup must be documented. An unprefixed line in application.properties is not
# itself a read, but it does supply the default that NFR-005 compares against. A key is
# "documented" if it appears in an \opt{...} in the manual; its documented default is the
# \val{...} that follows it in the same table row.
#
# Exit status is 0 when nothing drifted and 1 when something did, so it can gate a release.
#
# Usage: docs/manual/check-properties.sh [--quiet]

set -euo pipefail

MANUAL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "${MANUAL_DIR}/../.." && pwd)"
TEX="${MANUAL_DIR}/elicit-installation-manual.tex"

QUIET=0
[[ "${1:-}" == "--quiet" ]] && QUIET=1

for m in Survey Admin Author; do
  if [[ ! -d "${REPO_DIR}/${m}/src/main" ]]; then
    echo "[check] ${m} is not cloned — run cloneAllProjects.sh first." >&2
    exit 1
  fi
done

python3 - "${REPO_DIR}" "${TEX}" "${QUIET}" <<'PY'
import os, re, sys

repo, tex_path, quiet = sys.argv[1], sys.argv[2], sys.argv[3] == "1"
MODULES = ("Survey", "Admin", "Author")

# Keys the manual deliberately does not list one by one. Each entry is a prefix.
# Observability, telemetry and Vaadin plumbing are covered as a group in the manual and
# pointed at quarkus.io; listing ~150 individual otel/micrometer keys would make the
# reference unreadable without making it more useful.
IGNORED_PREFIXES = (
    "quarkus.otel.", "quarkus.micrometer.", "quarkus.log.category.",
    "quarkus.index-dependency.", "quarkus.container-image.", "quarkus.package.",
    "quarkus.jacoco.", "quarkus.banner.", "quarkus.http.auth.permission.",
    "quarkus.http.header.", "quarkus.flyway.owner.placeholders.",
    "quarkus.hibernate-orm.\"owner\".", "quarkus.hibernate-orm.owner.",
    "quarkus.datasource.devservices.", "quarkus.datasource.owner.devservices.",
    "quarkus.transaction-manager.", "quarkus.http.test-port",
    "quarkus.http.cors.enabled", "quarkus.http.cors.access-control-max-age",
    "quarkus.datasource.metrics.enabled", "quarkus.datasource.jdbc.telemetry",
    "quarkus.datasource.jdbc.enable-metrics", "quarkus.datasource.jdbc.metrics.enabled",
    "quarkus.datasource.jdbc.validation-query-sql",
    "quarkus.hibernate-orm.packages", "quarkus.hibernate-orm.log.",
    "quarkus.hibernate-orm.fetch.", "quarkus.smallrye-health.enabled",
    "quarkus.application.version", "quarkus.http.access-log.pattern",
    "quarkus.http.proxy.enable-forwarded-",
)

CONFIGPROP = re.compile(
    r'@ConfigProperty\(\s*name\s*=\s*"([^"]+)"(?:\s*,\s*defaultValue\s*=\s*"([^"]*)")?'
)
LOOKUP = re.compile(r'get(?:Optional)?Value\(\s*"([^"]+)"')
PROPLINE = re.compile(r'^([A-Za-z][A-Za-z0-9_."\-]*)\s*=\s*(.*)$')

def ignored(key):
    return any(key.startswith(p) for p in IGNORED_PREFIXES)

# A ${VAR:default} expression in application.properties supplies `default`; ${VAR} with no
# default supplies nothing. That indirection is how a deployment overrides the value, and the
# manual prints the default, not the expression.
ENVEXPR = re.compile(r'^\$\{[A-Za-z_][A-Za-z0-9_]*:(.*)\}$')

def resolve(value):
    if value is None:
        return None
    m = ENVEXPR.match(value.strip())
    if m:
        return m.group(1).strip()
    if value.strip().startswith("${") and value.strip().endswith("}"):
        return None
    return value.strip()

# ---------------------------------------------------------------- what the code reads
read = {}          # key -> {module: default-or-None}  (everything, for the default check)
required = set()   # keys read through @ConfigProperty or a programmatic lookup (NFR-004)

def note(key, module, default, is_read=False):
    if ignored(key):
        return
    if is_read:
        required.add(key)
    read.setdefault(key, {})
    default = resolve(default)
    if default is not None or module not in read[key]:
        read[key][module] = default

for module in MODULES:
    java_root = os.path.join(repo, module, "src", "main", "java")
    for dirpath, _dirs, files in os.walk(java_root):
        for f in files:
            if not f.endswith(".java"):
                continue
            text = open(os.path.join(dirpath, f), encoding="utf-8", errors="replace").read()
            for key, dflt in CONFIGPROP.findall(text):
                note(key, module, dflt if dflt != "" else None, is_read=True)
            for key in LOOKUP.findall(text):
                note(key, module, None, is_read=True)

    props = os.path.join(repo, module, "src", "main", "resources", "application.properties")
    if os.path.exists(props):
        for line in open(props, encoding="utf-8", errors="replace"):
            line = line.strip()
            if not line or line.startswith("#") or line.startswith("%"):
                continue
            m = PROPLINE.match(line)
            if m:
                note(m.group(1), module, m.group(2).strip())

# ---------------------------------------------------------------- what the manual documents
tex = open(tex_path, encoding="utf-8", errors="replace").read()
documented = set(re.findall(r'\\opt\{([^}]*)\}', tex))
# An \opt{} is a setting name only when it looks like one: dotted, or an upper-case
# environment variable. Paths such as /q/health/ready also use \opt and are not settings.
documented = {
    d for d in documented
    if (("." in d and "/" not in d) or re.fullmatch(r"[A-Z][A-Z0-9_]+", d))
    and not d.startswith("\\%")          # a profile prefix, not a setting
    and not d.startswith("foo.")          # the name-mangling example in the text
}

# Documented default: the \val{...} in the default cell — the second cell of the row whose
# first cell names the setting. Only the reference tables count: an \opt{} in running prose
# is a mention, not a declaration, and the \val{} nearest it is usually about something else.
# The cell matters as much as the row. A description that lists a setting's allowed values
# ("reads \val{survey}, \val{admin} or \val{author}") would otherwise hand the first of
# them to a key whose default cell deliberately says "per app". A default cell with no
# \val{} claims no value, so there is nothing to compare and the row is skipped.
row_default = {}
for table in re.findall(r"\\begin\{settings\}.*?\\end\{settings\}", tex, re.S):
    for row in re.split(r"\\\\", table):
        keys = re.findall(r'\\opt\{([^}]*)\}', row)
        if not keys:
            continue
        # Cells are separated by an unescaped &; \& is a literal ampersand within one.
        cells = re.split(r'(?<!\\)&', row)
        if len(cells) < 2:
            continue
        vals = re.findall(r'\\val\{([^}]*)\}', cells[1])
        if vals:
            row_default.setdefault(keys[0], vals[0].replace("\\&", "&"))

# ---------------------------------------------------------------- compare
undocumented = sorted(k for k in required if k not in documented)

# A Quarkus key the images leave at Quarkus's own default is still worth documenting, so an
# unread key is only a problem when it is one of Elicit's own.
ELICIT_PREFIXES = ("elicit.", "brand.", "i18n.", "accessCode.", "author.", "admin.", "build.")
stale = sorted(
    d for d in documented
    if d not in read
    and d.startswith(ELICIT_PREFIXES)
    and not re.fullmatch(r"[A-Z][A-Z0-9_]+", d)
)

mismatches = []
for key, doc_default in sorted(row_default.items()):
    if key not in read:
        continue
    actuals = {v for v in read[key].values() if v is not None}
    if not actuals:
        continue
    # "per app", "build", "---" and similar are deliberate prose, not a claimed value.
    if doc_default in ("per app", "build", "---", "empty", "as above"):
        continue
    if doc_default in actuals:
        continue
    # A value too long or too punctuated for a table cell may be given in full in a
    # verbatim block instead; the cell then carries a readable short form. That still
    # documents the default, so accept it when the full value appears anywhere in the
    # manual and the cell is a prefix of it.
    if any(a.startswith(doc_default) and a[len(doc_default):] in tex for a in actuals):
        continue
    mismatches.append((key, doc_default, sorted(actuals)))

problems = 0

def report(title, lines):
    global problems
    if not lines:
        return
    problems += len(lines)
    print(f"\n[check] {title} ({len(lines)}):")
    for l in lines:
        print(f"  {l}")

report("keys the code reads but the manual does not document",
       [f"{k}  (read by {', '.join(sorted(read[k]))})" for k in undocumented])
report("Elicit keys the manual documents that nothing reads", stale)
report("documented defaults that disagree with the code",
       [f"{k}: manual says {d!r}, code says {', '.join(repr(a) for a in acts)}"
        for k, d, acts in mismatches])

if problems == 0:
    if not quiet:
        print(f"[check] no drift: {len(documented)} documented keys cover "
              f"{len(required)} keys read through @ConfigProperty or a lookup across "
              f"{', '.join(MODULES)}.")
    sys.exit(0)

print(f"\n[check] {problems} difference(s). Update the manual, or add a prefix to "
      f"IGNORED_PREFIXES in this script if the key is deliberately covered as a group.")
sys.exit(1)
PY
