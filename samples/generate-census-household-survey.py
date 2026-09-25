#!/usr/bin/env python3
"""Generate the Census Household Survey .elicit definition.

Emits ELICIT_SURVEY_EXPORT_V1 (pipe-delimited, see Admin SurveyDefinitionExportService).
Element keys are uuid5 so regenerating produces a byte-identical file.
"""
import uuid, datetime

NS = uuid.UUID("6ba7b811-9dad-11d1-80b4-00c04fd430c8")
key = lambda s: str(uuid.uuid5(NS, "elicit/census-household-survey/" + s))

FROM, TO = "1970-01-01T00:00:00Z", "9999-12-31T23:59:59Z"
TAIL = ["0", FROM, TO, "", ""]          # version|effective_from|effective_to|published_by|published_comment

def esc(v):
    if v is None: return ""
    return str(v).replace("\\", "\\\\").replace("|", "\\|").replace("\n", "\\n").replace("\r", "\\r")

rows = {t: [] for t in ("surveys","select_groups","select_items","steps","sections",
                        "steps_sections","questions","sections_questions","relationships")}
def add(table, *fields): rows[table].append([esc(f) for f in fields])

# ---- question types (V003 ids, plus V018's TIME_PICKER/MODAL) ----
CHECKBOX,DATE_PICKER,COMBOBOX,HTML,INTEGER,DOUBLE,RADIO,TEXT = 1,2,3,4,5,6,7,8
TEXTAREA,MULTI_SELECT,CHECKBOX_GROUP,DATE_TIME_PICKER,EMAIL,PASSWORD = 9,10,11,12,13,14
TIME_PICKER,MODAL = 15,16
# ---- operators / actions ----
BOOLEAN,GREATER_THAN,EQUAL,NOT_EQUAL,FIELD_EXIST,CONTAINS = 1,2,3,4,5,6
SHOW,REPEAT,TEXT_ACTION = 1,2,3

# ---------------- select groups ----------------
GROUPS = {
 "Gender":        [("Male","MALE"),("Female","FEMALE"),("Another gender","OTHER"),("Prefer not to say","NOANS")],
 "MaritalStatus": [("Single, never married","SINGLE"),("Married","MARRIED"),("Separated","SEPARATED"),
                   ("Divorced","DIVORCED"),("Widowed","WIDOWED")],
 "Race":          [("White","WHITE"),("Black or African American","BLACK"),
                   ("American Indian or Alaska Native","AMIND"),("Asian","ASIAN"),
                   ("Native Hawaiian or Other Pacific Islander","NHPI"),("Some other race or origin","OTHER")],
 "Languages":     [("English","ENGLISH"),("Spanish","SPANISH"),("Arabic","ARABIC"),
                   ("Chinese","CHINESE"),("Another language","OTHER")],
 "Tenure":        [("Owned by you or someone in this household","OWN"),("Rented","RENT"),
                   ("Occupied without payment of rent","NOPAY")],
 "Relationship":  [("Spouse or partner","SPOUSE"),("Child","CHILD"),("Parent","PARENT"),
                   ("Brother or sister","SIBLING"),("Roommate or housemate","ROOMMATE"),("Other relative","OTHER")],
}
gid, iid = {}, 0
for n, (name, items) in enumerate(GROUPS.items(), start=1):
    gid[name] = n
    add("select_groups", n, key(f"group/{name}"), name, f"{name} choices", "Text", *TAIL)
    for order, (display, coded) in enumerate(items, start=1):
        iid += 1
        add("select_items", iid, key(f"item/{name}/{coded}"), n, display, order, coded, *TAIL)

# ---------------- steps ----------------
STEPS = [(1,"Welcome"),(2,"About You"),(3,"Your Home"),(4,"Household Members"),
         (5,"{name\u007Cthis person}"),(6,"Finishing Up")]
for n,(order,name) in enumerate(STEPS, start=1):
    add("steps", n, key(f"step/{order}"), order, name, "", f"{name} step", *TAIL)

# ---------------- sections ----------------
SECTIONS = ["Introduction","About you","Race and language","Housing","Rent details","Vehicles",
            "Vehicle","Household members","{name\u007Cthis person}","Contact","Anything else"]
for n,name in enumerate(SECTIONS, start=1):
    add("sections", n, key(f"section/{n}"), n, name, "", f"{name} section", *TAIL)

# ---------------- steps_sections (step -> sections mounted in it) ----------------
MOUNTS = [(1,[1]), (2,[2,3]), (3,[4,5,6,7]), (4,[8]), (5,[9]), (6,[10,11])]
ss = {}   # (step, section) -> steps_sections source_id
n = 0
for step, secs in MOUNTS:
    for within, sec in enumerate(secs, start=1):
        n += 1; ss[(step,sec)] = n
        dk = f"0001-{step:04d}-0000-{within:04d}-0000-0000-0000"
        add("steps_sections", n, key(f"ss/{step}/{sec}"), step, step, sec, within, dk, *TAIL)

# ---------------- questions ----------------
# (label, type, text, short, required, min, max, validation, group, placeholder, variant)
Q = [
 ("welcome",  HTML, "<h1>Census Household Survey</h1><p>This short survey asks about the people living "
   "in your home, the home itself, and how to reach you. It is a demonstration survey: it exercises every "
   "question type and every rule the Elicit engine supports.</p><p>Your answers save as you go, so you can "
   "stop and come back at any time.</p>", "Welcome", False,None,None,None,None,None,None),
 ("consent",  CHECKBOX, "I have read the above and agree to take part in this survey.", "Consent",
   True,None,None,None,None,None,None),
 ("name",     TEXT, "What is your full name?", "Name", True,1,100,"Please enter your name.",None,None,None),
 ("age",      INTEGER, "How old are you?", "Age", True,0,120,"Please enter an age between 0 and 120.",None,None,None),
 ("gender",   RADIO, "What is your gender?", "Gender", True,None,None,"Please select a value.","Gender",None,"vertical"),
 ("marital",  COMBOBOX, "What is your marital status?", "Marital status", False,None,None,None,"MaritalStatus",
   "Select one",None),
 ("race",     CHECKBOX_GROUP, "Which of the following describe you? Select all that apply.", "Race",
   False,None,None,None,"Race",None,"vertical"),
 ("raceother",TEXT, "Which other race or origin?", "Other race", False,None,100,None,None,None,None),
 ("langs",    MULTI_SELECT, "Which languages are spoken in this home?", "Languages",
   False,None,None,None,"Languages","Select all that apply",None),
 ("tenure",   RADIO, "Is this home owned or rented?", "Tenure", True,None,None,"Please select a value.","Tenure",None,"vertical"),
 ("movein",   DATE_PICKER, "About when did you move into this home?", "Move-in date", False,None,None,None,None,None,None),
 ("subsidy",  CHECKBOX, "Is any part of your housing cost subsidized?", "Subsidized", False,None,None,None,None,None,None),
 # The unit is named in the question text and echoed in the placeholder. questions.mask is
 # deliberately unused: ElicitComponent.setInputMask is private and never called, so a mask
 # here would be inert, and it is a character filter rather than a currency format anyway.
 ("rent",     DOUBLE, "About how much is the monthly rent, in US dollars?", "Monthly rent", False,0,20000,
   "Please enter an amount between 0 and 20000.",None,"0.00",None),
 ("vehcount", INTEGER, "How many cars, vans or trucks are kept at this home?", "Vehicle count",
   False,0,10,"Please enter a number between 0 and 10.",None,None,None),
 ("vehicle",  TEXT, "What is the make and model of this vehicle?", "Vehicle", False,None,100,None,None,
   "For example, Ford F-150",None),
 ("hhsize",   INTEGER, "Not counting yourself, how many other people live in this home?", "Household size",
   True,0,20,"Please enter a number between 0 and 20.",None,None,None),
 ("person",   TEXT, "What is the name of person {Q#}?", "Person name", False,None,100,None,None,None,None),
 ("personage",INTEGER, "How old is {name\u007Cthis person}?", "Person age", False,0,120,
   "Please enter an age between 0 and 120.",None,None,None),
 ("persongen",RADIO, "What is {name's\u007Cthis person's} gender?", "Person gender",
   False,None,None,None,"Gender",None,"vertical"),
 ("personrel",COMBOBOX, "What is {name's\u007Cthis person's} relationship to {proband\u007Cyou}?", "Relationship",
   False,None,None,None,"Relationship","Select one",None),
 ("email",    EMAIL, "What email address should we use to confirm your response?", "Email",
   False,None,None,"Please enter a valid email address.",None,"you@example.com",None),
 ("leave",    TIME_PICKER, "What time do you usually leave home for work?", "Departure time",
   False,None,None,None,None,None,None),
 ("followup", DATE_TIME_PICKER, "If we need to follow up, what date and time works best?", "Follow-up",
   False,None,None,None,None,None,None),
 ("comments", TEXTAREA, "Is there anything else about your household you would like to tell us?", "Comments",
   False,None,2000,None,None,None,None),
 ("thanks",   MODAL, "<p>Thank you for completing the Census Household Survey.</p>"
   "<p>Your answers have been saved. You may close this window.</p>", "Thank you",
   False,None,None,None,None,None,None),
]
qid = {}
for n,(label,typ,text,short,req,mn,mx,val,grp,ph,var) in enumerate(Q, start=1):
    qid[label] = n
    add("questions", n, key(f"question/{label}"), typ, text, short, "", str(req).lower(),
        mn, mx, val, gid[grp] if grp else None, "", ph, "", var, *TAIL)

# ---------------- sections_questions (question -> section) ----------------
ASSIGN = [
 (1, ["welcome","consent"]),
 (2, ["name","age","gender","marital"]),
 (3, ["race","raceother","langs"]),
 (4, ["tenure","movein","subsidy"]),
 (5, ["rent"]),
 (6, ["vehcount"]),
 (7, ["vehicle"]),
 (8, ["hhsize","person"]),
 (9, ["personage","persongen","personrel"]),
 (10,["email","leave","followup"]),
 (11,["comments","thanks"]),
]
sq = {}   # label -> sections_questions source_id
n = 0
for sec, labels in ASSIGN:
    for order, label in enumerate(labels, start=1):
        n += 1; sq[label] = n
        add("sections_questions", n, key(f"sq/{sec}/{label}"), qid[label], sec, order, *TAIL)

# ---------------- relationships ----------------
# up_step, up_sq, down_step, down_ss, down_sq, operator, action, description, token, ref, default
# A rule whose target sits in the upstream question's OWN section is stored question-only:
# upstream_step, downstream_step and downstream_ss are all null (see Author RulePath.complete).
RULES = [
 (1, sq["consent"],  2, None, None,              BOOLEAN,      SHOW,        "Show About You once consent is given", "", "", ""),
 (None, sq["race"],  None, None, sq["raceother"],CONTAINS,     SHOW,        "Ask which other race",              "", "OTHER", ""),
 (3, sq["tenure"],   3, ss[(3,5)], None,         EQUAL,        SHOW,        "Show rent details when renting",    "", "RENT", ""),
 (None, sq["tenure"],None, None, sq["subsidy"],  NOT_EQUAL,    SHOW,        "Ask about subsidy unless owned",    "", "OWN", ""),
 (3, sq["vehcount"], 3, ss[(3,7)], None,         GREATER_THAN, REPEAT,      "Repeat the vehicle section",        "", "0", ""),
 (None, sq["hhsize"],None, None, sq["person"],   GREATER_THAN, REPEAT,      "Repeat the person name question",   "", "0", ""),
 (4, sq["person"],   5, None, None,              FIELD_EXIST,  SHOW,        "Show a step per household member",  "", "", ""),
 (4, sq["person"],   5, None, None,              FIELD_EXIST,  TEXT_ACTION, "Fill {name} for this member",   "name", "", ""),
 (2, sq["name"],     5, None, None,              FIELD_EXIST,  TEXT_ACTION, "Fill {proband} with your name","proband", "", ""),
 # Chain the remaining steps, so nothing past Welcome is reachable until consent is given.
 # Without these, steps 3, 4 and 6 are ungated and appear in navigation from the first page.
 (2, sq["gender"],   3, None, None,              FIELD_EXIST,  SHOW,        "Show Your Home",                    "", "", ""),
 (3, sq["tenure"],   4, None, None,              FIELD_EXIST,  SHOW,        "Show Household Members",            "", "", ""),
 (4, sq["hhsize"],   6, None, None,              FIELD_EXIST,  SHOW,        "Show Finishing Up",                 "", "", ""),
]
for n,(us,usq,ds,dss,dsq,op,act,desc,tok,ref,dflt) in enumerate(RULES, start=1):
    add("relationships", n, key(f"rule/{n}"), us, usq, ds, dss, dsq, op, act, desc, tok, ref, dflt, "", *TAIL)

# ---------------- survey ----------------
add("surveys", 1, key("survey"), "Census Household Survey", 2, "Census Household Survey",
    "A demonstration survey about census household data. It uses every question type and every "
    "rule the Elicit engine supports, exactly once where practical.",
    "0001-0001-0000-0001-0000-0000-0000", "", "", "")

# ---------------- emit ----------------
ORDER = ["surveys","select_groups","select_items","steps","sections","steps_sections",
         "questions","sections_questions","relationships"]
EXPECTED = {"surveys":10,"select_groups":10,"select_items":11,"steps":11,"sections":11,
            "steps_sections":12,"questions":20,"sections_questions":10,"relationships":19}
for t in ORDER:
    for r in rows[t]:
        assert len(r) == EXPECTED[t], f"{t}: {len(r)} fields, expected {EXPECTED[t]}: {r}"

now = "2026-09-25T12:45:00Z"
out = ["# ELICIT_SURVEY_EXPORT_V1", "# survey_id: 1", f"# survey_key: {key('survey')}",
       "# survey_name: Census Household Survey"]
for t in ORDER + ["reports","post_survey_actions","dimensions","ontology","metadata"]:
    out.append(f"# {t}: {len(rows.get(t, []))}")
out += [f"# generated: {now}", f"# survey_revision: {now}", ""]
for t in ORDER:
    for r in rows[t]:
        out.append(f"{t}: " + "|".join(r))

import sys
open(sys.argv[1], "w").write("\n".join(out) + "\n")
print(f"wrote {sys.argv[1]}: " + ", ".join(f"{t}={len(rows[t])}" for t in ORDER))
