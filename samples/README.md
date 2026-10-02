# Sample Survey Definitions

## `census-household-survey.elicit`

A demonstration survey about census household data, built to exercise **every
question type and every rule the Elicit engine supports**. It is small enough to
walk end to end in a few minutes, which makes it useful as a smoke test after a
schema or runtime change, and as a worked reference when authoring a new survey.

Install it with **Admin > Apply Survey Definition**, or:

```sh
curl -u <admin> -F "file=@samples/census-household-survey.elicit" \
  http://localhost:8081/secured/survey/apply
```

It carries `display_order` 2, so it installs alongside the Family History Survey
rather than colliding with it.

### Reporting tags

The sample is tagged for the reporting star schema to the authoring guidelines in
`docs/research/faceted_exploration.md` (section 4): two dimensions, `age` and `gender`, each
role-played by a respondent tag and a household-member tag (`Age` / `Member Age`,
`Gender` / `Member Gender`); `Marital Status`, `Tenure`, `Subsidized`, `Vehicle Count`,
`Household Size` and `Member Relationship` on their own tables; every tag attached at question
scope and reporting the answer. Race (a CHECKBOX_GROUP) and Languages (a MULTI_SELECT) are
deliberately untagged — a multi-choice answer is stored comma-joined and would report every
combination as one value — and free text, dates, the rent amount and contact details are never
tagged. The speakers question repeated once per language is a repeated *question*, so its
instances share one fact row and nothing names the language in reporting; only a *section*
repeated per selected item gets the `question` / `item` columns of
`docs/research/per_survey_reporting_schema.md` 3.6. Step and section `dimension_name`s are set explicitly: `Household Member` for the
per-member step, whose display name is a token phrase, and a `Census` prefix on the rest so they
cannot collide with another survey's `Welcome` on the same site.

### Coverage

| | Covered |
| --- | --- |
| Question types | 15/16 — every row in `survey.question_types` except `PASSWORD` |
| Operators | 6/6 — `BOOLEAN`, `GREATER THAN`, `EQUAL`, `NOT_EQUAL`, `FIELD_EXIST`, `CONTAINS` |
| Actions | 3/3 — `SHOW`, `REPEAT`, `TEXT` |
| Rule targets | step, section (`downstream_ss_id`), and question (`downstream_sq_id`) |
| Repeat sources | a count (`INTEGER`, instances 1 to N) and a selection (`MULTI_SELECT`, one instance per selected item) |

Two deliberate omissions:

- **`PASSWORD`** — a census household survey has no honest use for a password field, and the
  one candidate (a PIN to resume later) is not a real feature of the platform. Exercising the
  type was not worth inventing a question nobody would ask.
- **`LESS THAN`** — implemented in `Relationship.evaluateOperator` but seeded by no migration,
  so no definition can reference it.

### Structure

| Step | Sections | Notable elements and rules |
| --- | --- | --- |
| 1 Welcome | Introduction | `HTML` intro + `CHECKBOX` consent. Consent gates step 2 via `BOOLEAN`. |
| 2 About You | About you; Race and language | `TEXT`, `INTEGER`, `RADIO`, `COMBOBOX`, `CHECKBOX_GROUP`, `MULTI_SELECT`. Race `CONTAINS 'OTHER'` reveals a follow-up in the **same section** (the question-only rule path). The languages `MULTI_SELECT` `REPEAT`s a speakers question **once per language selected**, numbered by the language's position in its list, with the `LANG` token filled by each language's own text. |
| 3 Your Home | Housing; Rent details; Vehicles; Vehicle | `DATE_PICKER`, `DOUBLE`. Tenure `EQUAL 'RENT'` shows the **Rent details section**; tenure `NOT_EQUAL 'OWN'` shows a question. Vehicle count `REPEAT`s the **Vehicle section**. |
| 4 Household Members | Household members | Household size `REPEAT`s the person-name **question** in its own section. |
| 5 `{name\|this person}` | `{name\|this person}` | Shown once per person name via `FIELD_EXIST`; a `TEXT` rule fills the `name` token across the step, its section and its questions. |
| 6 Finishing Up | Contact; Anything else | `EMAIL`, `TIME_PICKER`, `DATE_TIME_PICKER`, `TEXTAREA`, and a `MODAL` thank-you. |

### Requirements

`TIME_PICKER` and `MODAL` are seeded by **Survey migration V018**, which also
renames `DATETIME` to `DATE_TIME_PICKER` so the existing render case fires. On a
database without V018 the definition will fail to import, because `questions.type_id`
is carried verbatim and ids 15 and 16 will not exist.

### Configuration this sample deliberately does not set

`questions.mask` is left empty everywhere. `ElicitComponent.setInputMask` is `private` and
has no callers in Survey, so a mask never reaches a component; it also targets
`HasAllowedCharPattern`, which filters typeable characters rather than formatting a value.
The rent question names its unit in the question text and shows a `0.00` placeholder
instead. Currency formatting proper is a separate design question — the currency *code*
belongs to the survey (a respondent reading Spanish in Michigan still pays USD), while
symbol placement and separators belong to the viewer's locale, and Vaadin's `NumberField`
offers no formatting hook for either.

### Token mechanics worth copying

- Placeholders are `{phrase|default}` and match by **containment**, so one rule
  declaring the token `NAME` fills both `{<NAME>|this person}` and
  `{<NAME>'s|this person's}`. A token is written `<NAME>` where it is used --
  upper case, in angle brackets, inside the placeholder's phrase -- so a rule
  fills a reference and never a word that merely reads like one.
- What a token holds follows from the question its rule reads (Survey UC-002
  BR-010): a free-text, date or time answer fills it with the respondent's own
  words, and a coded, numeric or boolean answer yields to the constant written on
  the rule. `NAME` and `PROBAND` are both fed by `TEXT` questions.
- `LANG` is the third case (Survey UC-002 BR-013). Inside an instance that a
  `REPEAT` builds once per selected item, a token filled from that same
  `MULTI_SELECT` holds the item's display text, translated where the survey
  publishes a translation of it. The speakers question leads with its placeholder
  (`{<LANG>|This language}: about how many...`) so that every item of the list
  reads well in it.
- `{Q#}` and `{S#}` are instance counters the runtime fills itself; they are not
  rule tokens and need no rule.

## Tooling

- `generate-census-household-survey.py <out>` — regenerates the definition.
  Element keys are `uuid5`, so output is byte-identical across runs. Edit this
  rather than the pipe-delimited file.
- `validate-elicit.py <file>` — replays the import and export validations
  (header, table order, field counts, FK resolution, choice types needing a
  select group, `initial_display_key`, rule direction, unfilled tokens) without
  needing a database. Works on any `.elicit` file.
