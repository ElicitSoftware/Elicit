#!/usr/bin/env python3
"""Write a keyless template of a definition file.

A definition's survey_key and element keys are what make two installs the same survey: a site
updates by them. A survey built from someone else's file would carry that file's keys and read
as a revision of it. The template has every key cleared, so Author's Import definition mints
new ones and the survey that results is related to nothing.

The template is marked ELICIT_SURVEY_DRAFT_V1 (Author C-022): Admin does not apply it, so it
reaches a site only as an export from the Author that minted its keys.

    samples/make-template.py <definition.elicit> <template.elicit>
"""
import sys, re

EXPORT, DRAFT = "ELICIT_SURVEY_EXPORT_V1", "ELICIT_SURVEY_DRAFT_V1"
# Every record but a translation is <table>: <source id>|<key>|...
KEYED = re.compile(r"^([a-z_]+: \d+\|)[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}(\|)")

if len(sys.argv) != 3:
    sys.exit(__doc__)
src, dst = sys.argv[1], sys.argv[2]

out, cleared, marked = [], 0, False
with open(src, encoding="utf-8", newline="") as f:
    for n, line in enumerate(f, 1):
        if line.startswith("#"):
            if EXPORT in line:
                line, marked = line.replace(EXPORT, DRAFT), True
            elif line.startswith("# survey_key:"):
                continue
        elif line.strip():
            if line.startswith("translations:"):
                # A translation names its element by key; with the keys cleared it names nothing.
                sys.exit(f"{src}:{n}: a definition with translations cannot be made keyless")
            line, hits = KEYED.subn(r"\1\2", line)
            if not hits:
                sys.exit(f"{src}:{n}: no key in the second field")
            cleared += 1
        out.append(line)
if not marked:
    sys.exit(f"{src}: no {EXPORT} header line")

with open(dst, "w", encoding="utf-8", newline="") as f:
    f.writelines(out)
print(f"{dst}: {cleared} keys cleared")
