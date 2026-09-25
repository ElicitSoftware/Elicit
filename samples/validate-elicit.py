#!/usr/bin/env python3
"""Replay the .elicit import + export validations against a definition file."""
import sys, re, collections

COUNTS = {"surveys":10,"select_groups":10,"select_items":11,"steps":11,"sections":11,
          "steps_sections":12,"questions":20,"sections_questions":10,"relationships":19,
          "reports":6,"post_survey_actions":6,"dimensions":3,"ontology":5,"metadata":7}
ORDER = ["surveys","select_groups","select_items","steps","sections","steps_sections",
         "questions","sections_questions","relationships","reports","post_survey_actions",
         "dimensions","ontology","metadata"]
CHOICE_TYPES = {7,3,10,11}          # RADIO, COMBOBOX, MULTI_SELECT, CHECKBOX_GROUP
PLACEHOLDER = re.compile(r"\{([^{}|]+)(?:\|([^{}]*))?}")
BUILTIN = {"Q#","S#"}

def split(line):
    out, cur, i = [], "", 0
    while i < len(line):
        c = line[i]
        if c == "\\" and i+1 < len(line):
            nxt = line[i+1]
            cur += {"|":"|","\\":"\\","n":"\n","r":"\r"}.get(nxt, "\\"+nxt); i += 2; continue
        if c == "|": out.append(cur); cur = ""; i += 1; continue
        cur += c; i += 1
    out.append(cur)
    return out

errs, warns = [], []
rows = collections.defaultdict(list)
seen_order, header_ok = [], False
for n, raw in enumerate(open(sys.argv[1]), 1):
    line = raw.rstrip("\n")
    if line.startswith("#"):
        if "ELICIT_SURVEY_EXPORT_V1" in line: header_ok = True
        continue
    if not line.strip(): continue
    if ":" not in line: errs.append(f"L{n}: missing colon separator"); continue
    table, _, rest = line.partition(": ")
    if table not in COUNTS: errs.append(f"L{n}: unknown table {table!r}"); continue
    f = split(rest)
    if len(f) != COUNTS[table]:
        errs.append(f"L{n}: {table} requires {COUNTS[table]} fields, got {len(f)}")
    rows[table].append((n, f))
    if table not in seen_order: seen_order.append(table)

if not header_ok: errs.append("no ELICIT_SURVEY_EXPORT_V1 header line")
if seen_order and seen_order[0] != "surveys": errs.append(f"first table is {seen_order[0]}, must be surveys")
if seen_order != [t for t in ORDER if t in seen_order]:
    errs.append(f"tables out of dependency order: {seen_order}")
if len(rows["surveys"]) != 1: errs.append(f"expected exactly 1 survey record, got {len(rows['surveys'])}")
if rows["surveys"] and not rows["surveys"][0][1][1].strip():
    errs.append("survey_key is blank; Apply rejects a keyless file")

ids = {t: {f[0] for _, f in rs} for t, rs in rows.items()}
def fk(table, idx, target, label):
    for n, f in rows[table]:
        v = f[idx]
        if v and v not in ids.get(target, set()):
            errs.append(f"L{n}: {table}.{label}={v} does not resolve to a {target} source_id")

fk("select_items", 2, "select_groups", "select_group_id")
fk("steps_sections", 2, "steps", "step_id"); fk("steps_sections", 4, "sections", "section_id")
fk("questions", 10, "select_groups", "select_group_id")
fk("sections_questions", 2, "questions", "question_id"); fk("sections_questions", 3, "sections", "section_id")
for idx, tgt, lbl in ((2,"steps","upstream_step_id"),(3,"sections_questions","upstream_sq_id"),
                      (4,"steps","downstream_step_id"),(5,"steps_sections","downstream_ss_id"),
                      (6,"sections_questions","downstream_sq_id")):
    fk("relationships", idx, tgt, lbl)

# choice types need a select group (Author ExportValidation)
qtype, qtext = {}, {}
for n, f in rows["questions"]:
    qtype[f[0]] = int(f[2]) if f[2] else 0
    qtext[f[0]] = f[3]
    if not f[3].strip(): errs.append(f"L{n}: question {f[0]} has empty text")
    if qtype[f[0]] in CHOICE_TYPES and not f[10].strip():
        errs.append(f"L{n}: question {f[0]} is a choice type ({qtype[f[0]]}) with no select_group_id")

# initial_display_key must name a real mount
dks = {f[6] for _, f in rows["steps_sections"]}
if rows["surveys"]:
    idk = rows["surveys"][0][1][6]
    if idk not in dks: errs.append(f"initial_display_key {idk!r} matches no steps_sections.display_key")

# rule direction: downstream must come after upstream
step_order = {f[0]: int(f[2]) for _, f in rows["steps"]}
sec_of_sq = {f[0]: f[3] for _, f in rows["sections_questions"]}
ord_of_sq = {f[0]: int(f[4]) for _, f in rows["sections_questions"]}
mount = {f[0]: (int(f[3]), int(f[5])) for _, f in rows["steps_sections"]}   # ss -> (step order, section order within step)
sec_mount = {}
for _, f in rows["steps_sections"]: sec_mount.setdefault(f[4], []).append((int(f[3]), int(f[5])))

def place_sq(sq):
    sec = sec_of_sq.get(sq)
    ms = sec_mount.get(sec, [])
    return [(so, seco, ord_of_sq.get(sq, 0)) for so, seco in ms]

for n, f in rows["relationships"]:
    up_sq, d_step, d_ss, d_sq = f[3], f[4], f[5], f[6]
    if not (d_step or d_ss or d_sq):
        errs.append(f"L{n}: rule {f[0]} names no downstream target")
    ups = place_sq(up_sq)
    # The innermost element named is the target; the outer ones only say where it sits.
    if d_sq:
        for t in place_sq(d_sq):
            for u in ups:
                if t[:2] == u[:2] and t[2] <= u[2]:
                    errs.append(f"L{n}: rule {f[0]} targets question order {t[2]} at or before origin {u[2]}")
    elif d_ss:
        so, seco = mount[d_ss]
        for u in ups:
            if (so, seco) <= (u[0], u[1]):
                errs.append(f"L{n}: rule {f[0]} targets a section at or before its origin")
    elif d_step:
        tgt = step_order.get(d_step, 0)
        for u in ups:
            if tgt <= u[0]:
                errs.append(f"L{n}: rule {f[0]} targets step order {tgt} at or before its origin step {u[0]}")

# tokens: every placeholder either has a default or a rule filling it
tokens = {f[10] for _, f in rows["relationships"] if f[10].strip()}
texts = [(n, f[3]) for n, f in rows["steps"]] + [(n, f[3]) for n, f in rows["sections"]] \
        + [(n, f[3]) for n, f in rows["questions"]]
for n, t in texts:
    for m in PLACEHOLDER.finditer(t or ""):
        phrase, default = m.group(1), m.group(2)
        if phrase in BUILTIN: continue
        filled = any(tok in phrase for tok in tokens)
        if not filled and default is None:
            errs.append(f"L{n}: placeholder {m.group(0)!r} has no filling rule and no default")
        elif not filled:
            warns.append(f"L{n}: placeholder {m.group(0)!r} has no filling rule (falls back to default)")

print(f"tables: " + ", ".join(f"{t}={len(rows[t])}" for t in ORDER if rows[t]))
print(f"tokens declared by rules: {sorted(tokens)}")
for w in warns: print(f"  WARN  {w}")
for e in errs: print(f"  ERROR {e}")
print(("FAILED: %d error(s)" % len(errs)) if errs else "OK: all checks passed")
sys.exit(1 if errs else 0)
