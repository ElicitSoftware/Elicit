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

### Coverage

| | Covered |
| --- | --- |
| Question types | 16/16 — every row in `survey.question_types` |
| Operators | 6/6 — `BOOLEAN`, `GREATER THAN`, `EQUAL`, `NOT_EQUAL`, `FIELD_EXIST`, `CONTAINS` |
| Actions | 3/3 — `SHOW`, `REPEAT`, `TEXT` |
| Rule targets | step, section (`downstream_ss_id`), and question (`downstream_sq_id`) |

`LESS THAN` is deliberately absent: it is implemented in `Relationship.evaluateOperator`
but no migration seeds the row, so no definition can reference it.

### Structure

| Step | Sections | Notable elements and rules |
| --- | --- | --- |
| 1 Welcome | Introduction | `HTML` intro + `CHECKBOX` consent. Consent gates step 2 via `BOOLEAN`. |
| 2 About You | About you; Race and language | `TEXT`, `INTEGER`, `RADIO`, `COMBOBOX`, `CHECKBOX_GROUP`, `MULTI_SELECT`. Race `CONTAINS 'OTHER'` reveals a follow-up in the **same section** (the question-only rule path). |
| 3 Your Home | Housing; Rent details; Vehicles; Vehicle | `DATE_PICKER`, `DOUBLE`. Tenure `EQUAL 'RENT'` shows the **Rent details section**; tenure `NOT_EQUAL 'OWN'` shows a question. Vehicle count `REPEAT`s the **Vehicle section**. |
| 4 Household Members | Household members | Household size `REPEAT`s the person-name **question** in its own section. |
| 5 `{name\|this person}` | `{name\|this person}` | Shown once per person name via `FIELD_EXIST`; a `TEXT` rule fills the `name` token across the step, its section and its questions. |
| 6 Finishing Up | Contact; Anything else | `EMAIL`, `TIME_PICKER`, `DATE_TIME_PICKER`, `PASSWORD`, `TEXTAREA`, and a `MODAL` thank-you. |

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
  declaring the token `name` fills both `{name|this person}` and
  `{name's|this person's}`.
- Only `CHECKBOX`, `HTML`, `RADIO` and `TEXT` answers actually fill a token —
  `QuestionManager.getKeyValues` switches on type names and falls through for
  `COMBOBOX`, `INTEGER` and the rest. Both tokens here are fed by `TEXT` questions.
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
