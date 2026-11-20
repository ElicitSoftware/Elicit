# Findings from the Census Household Survey

Issues surfaced by walking `census-household-survey.elicit` end to end on 2026-09-25,
against Survey built from `V3` plus migration `V018`. The sample exists to exercise every
element and rule once, so these are the things that exercise turned up.

**Findings 1, 2, 3 and 4 were fixed on 2026-09-26**, because the multilingual multi-site journey
walks this survey nine times and could not get past any of them. Finding 4 turned out not to be
a navigation stall at all; see its section. Finding 5 stands.

Line references are to the `V3` branch of `Survey/`.

---

## 1. COMBOBOX answers are stored as a Java object identity hash

**Severity: high — silent data corruption, affects any survey with a combobox. Fixed 2026-09-26.**

Observed in `survey.answers` after answering the marital-status question:

```
Marital status | COMBOBOX | com.elicitsoftware.model.SelectItem@26c9981e
```

`SectionView.java:202` saves the combobox value with `e.getValue().toString()`:

```java
case GlobalStrings.QUESTION_TYPE_COMBOBOX:
    ElicitComboBox comboBox = new ElicitComboBox(answer);
    comboBox.component.addValueChangeListener(e -> {
        saveAnswer(answer, e.getValue().toString());   // <- SelectItem, not codedValue
    });
```

`SelectItem` declares no `toString()`, so this persists `Object.toString()` — the class
name and an identity hash. Compare `RADIO` at `SectionView.java:247`, which correctly saves
`e.getValue().codedValue` (verified: gender saved `MALE`, tenure saved `RENT`).

Consequences: combobox answers are meaningless in `surveyreport`, render as the hash on the
Review page, and **no rule can ever match a combobox answer**, since `reference_value` is
compared against that string. The hash also changes between JVM runs.

`MULTI_SELECT` has a related but milder variant — `SectionView` joins `codedValue` with
commas in its own listener, while `Answer.getSelectedItems()` never fires for it.

**Fixed:** `SectionView` now saves `e.getValue().codedValue`, as `RADIO` does, with a null guard
for a cleared value. That is also what `ElicitComboBox.setValue` reads back, so a combobox answer
now survives the respondent leaving and returning — it never did before. Combobox answers written
before the fix are unrecoverable.

---

## 2. Same-section rules do not hide their target when a step is built

`QuestionManager` has two hiding queries. `sqlSection` (line 397) excludes question-only
rules through a `UNION` branch:

```sql
SELECT R.DOWNSTREAM_SQ_ID FROM SURVEY.RELATIONSHIPS R
 WHERE ... R.DOWNSTREAM_SS_ID IS NULL AND R.DOWNSTREAM_SQ_ID IS NOT NULL
```

`sqlStep` (line 388) has no equivalent branch — it only excludes questions whose rule has
`DOWNSTREAM_SS_ID IS NOT NULL`. A rule stored question-only (the shape `RulePath.complete`
produces when the target sits in the upstream question's own section) is therefore invisible
to the step-level query, and its target renders immediately.

Observed: "Which other race or origin?" (gated `CONTAINS 'OTHER'`) and "Is any part of your
housing cost subsidized?" (gated `NOT_EQUAL 'OWN'`) were both visible from the first render.
Section-level rules behave correctly — "Rent details" stayed hidden until tenure was `RENT`.

**Fixed 2026-09-26:** `sqlStep` now carries the same `UNION` branch as `sqlSection`.

---

## 3. There is no operator meaning "has been answered"

`FIELD_EXIST` returns `true` unconditionally (`Relationship.java`, `evaluateOperator`):
reaching the evaluator means an answer *row* exists, and rows are created when a step is
built, not when a respondent types something.

Using `FIELD_EXIST` to chain steps therefore fires every rule the moment the upstream step
materializes. In this sample that collapsed the whole chain at once — 44 answer rows with 12
actually filled in, and steps 4, 5 and 6 all built while their upstream questions were blank.

**The sample's own rules were reworked on 2026-09-26.** Steps 3, 4 and 6 are now gated on the
consent checkbox with `BOOLEAN`, an operator that actually reads a value, instead of being chained
one to the next on `FIELD_EXIST`. `FIELD_EXIST` is still used for the one thing it does express:
one step instance per REPEATed answer row (rules 7 and 8), which is the FHHS pattern.

The underlying gap is unchanged:
for a free-text question there is no way to express "show this once a value has been
entered". The seeded operators all test a *value* (`BOOLEAN`, `GREATER THAN`, `EQUAL`,
`NOT_EQUAL`, `CONTAINS`) or nothing at all (`FIELD_EXIST`).

---

## 4. Navigation stalls after a section REPEAT — two bugs, both fixed

**Severity: high — any survey with a REPEATed section was unfinishable. Fixed 2026-09-26.**

After answering "How many cars, vans or trucks" with `2`, the REPEAT correctly created two
Vehicle section instances (`0001-0003-0000-0004-0001-…` and `…-0004-0002-…`, each with its own
section-title row) — and then Next re-rendered the same page. It was never a navigation
*stall*: it was an exception, caught and shown as a notification, and then a missing button.

**4a. `SectionView.buildQuestions` dereferenced a null question.** The guard for the row that
carries a section's title read

```java
if (answer.question == null && answer.sectionInstance == 0) {   // section title
```

A section title is exactly the row of a section that carries no question; the instance number has
nothing to do with it. A REPEATed section's instances are numbered 1, 2, … and have a title row
each, so they fell through to the question branch and threw
`NullPointerException: Cannot invoke "Question.getQuestionType()" because "Answer.getQuestion()"
is null`. `nextSection()` caught it and showed "Error navigating to next section", which on a
page that had not changed looks exactly like a stall. The guard is now `answer.question == null`.

**4b. `DisplayKey.getSectionString()` zeroed the section instance.** With 4a fixed the section's
question rendered, but with no Previous/Next buttons and no title. `addButtons()` returns early
when `navResponse.getCurrentNavItem()` is null, and it was: `QuestionManager.getCurrentNavItem`
matched navigation items against `DisplayKey.getSectionString()`, which builds
`survey-step-stepInstance-section-**0000**-0000-0000`. Navigation items are built from the
respondent's own section-title answer rows, whose keys carry the real instance, so no item ever
matched a repeated section. That form is right where a key names a section's *structure* — a
placement is one row whichever instance is being looked at — so `getSectionString()` is unchanged
and a new `getSectionInstanceString()` was added for the navigation lookup.

Neither bug is specific to this sample: 4a hits any survey with a REPEATed section, and 4b hits
the second and later instances of one.

## 5. Smaller notes

- **`LESS THAN` is unreachable.** Implemented at `Relationship.java:266`, but no migration
  seeds the row in `survey.operator_types`, so no definition can reference it.
- **Token substitution is English-only.** `QuestionManager.replaceTokens` ends with
  hardcoded fix-ups (`" her's "` → `" her "`, `s's` → `s'`). These are wrong for any other
  language and run regardless of locale.
- **Token values only fill from some types.** `getKeyValues` switches on `CHECKBOX`,
  `DROPDOWN`, `HTML`, `NUMBER`, `RADIO`, `TEXT`, `DATE` — and `DROPDOWN`, `NUMBER` and
  `DATE` are not names that exist in `question_types`. A token fed by a `COMBOBOX`,
  `INTEGER` or `DATE_PICKER` answer silently stays empty.
