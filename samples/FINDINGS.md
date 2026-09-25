# Findings from the Census Household Survey

Issues surfaced by walking `census-household-survey.elicit` end to end on 2026-09-25,
against Survey built from `V3` plus migration `V018`. The sample exists to exercise every
element and rule once, so these are the things that exercise turned up. None are fixed.

Line references are to the `V3` branch of `Survey/` and `main` of `Author/`.

---

## 1. COMBOBOX answers are stored as a Java object identity hash

**Severity: high — silent data corruption, affects any survey with a combobox.**

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

**Fix:** save `codedValue`, as `RADIO` does. Existing combobox answers are unrecoverable.

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

**Fix:** add the same `UNION` branch to `sqlStep`.

---

## 3. There is no operator meaning "has been answered"

`FIELD_EXIST` returns `true` unconditionally (`Relationship.java`, `evaluateOperator`):
reaching the evaluator means an answer *row* exists, and rows are created when a step is
built, not when a respondent types something.

Using `FIELD_EXIST` to chain steps therefore fires every rule the moment the upstream step
materializes. In this sample that collapsed the whole chain at once — 44 answer rows with 12
actually filled in, and steps 4, 5 and 6 all built while their upstream questions were blank.

The sample's own rules were wrong here and need reworking. But the underlying gap is real:
for a free-text question there is no way to express "show this once a value has been
entered". The seeded operators all test a *value* (`BOOLEAN`, `GREATER THAN`, `EQUAL`,
`NOT_EQUAL`, `CONTAINS`) or nothing at all (`FIELD_EXIST`).

---

## 4. `questions.mask` is inert, and Author advertises it anyway

`ElicitComponent.setInputMask` (line 212) is `private` and has no callers anywhere in
`Survey/src`. The column round-trips through export and import and is never applied.

Author shows an "Input mask" field (`QuestionDialog.java:68`) with the help text *"A pattern
that constrains what the respondent can type."* — so the UI offers a control with no runtime
effect. It is shown only for text-like types (`QuestionDialog.java:291`).

Neither FHHS nor this sample sets a mask on any question.

Note that the method targets `HasAllowedCharPattern`, a character filter. It cannot display
a symbol, so it is not a route to currency formatting; Vaadin's `NumberField` has no format
hook, and its only currency affordance is `setPrefixComponent`.

---

## 5. Navigation stalls after a section REPEAT

After answering "How many cars, vans or trucks" with `2`, the REPEAT correctly created two
Vehicle section instances (`0001-0003-0000-0004-0001-…` and `…-0004-0002-…`, each with its
own section-title row). Navigation then would not advance past the Vehicles section — Next
re-rendered the same page.

Not diagnosed. It may be entangled with finding 3, since every later step had already been
built by then.

---

## 6. Smaller notes

- **`LESS THAN` is unreachable.** Implemented at `Relationship.java:266` and labelled in
  Author's `RelationshipQuery`, but no migration seeds the row, so no definition can
  reference it.
- **Token substitution is English-only.** `QuestionManager.replaceTokens` ends with
  hardcoded fix-ups (`" her's "` → `" her "`, `s's` → `s'`). These are wrong for any other
  language and run regardless of locale.
- **Token values only fill from some types.** `getKeyValues` switches on `CHECKBOX`,
  `DROPDOWN`, `HTML`, `NUMBER`, `RADIO`, `TEXT`, `DATE` — and `DROPDOWN`, `NUMBER` and
  `DATE` are not names that exist in `question_types`. A token fed by a `COMBOBOX`,
  `INTEGER` or `DATE_PICKER` answer silently stays empty.
