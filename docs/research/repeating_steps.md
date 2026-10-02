# Repeating Steps

> **Status (2026-10-02):** Research. The question was what it would take to let a whole step
> repeat, and whether `display_key` has a place for a step instance. **It does, and it is already
> in use.** The third of the key's seven segments is the step instance; `survey.answers` and
> `surveyreport.fact_sections` both carry a `step_instance` column; and step instances are created
> today whenever a SHOW rule on a step is fired by a repeated question (the pattern the Family
> History Survey uses for children and siblings). What was never implemented is narrower: the
> **REPEAT action on a step**. Survey's handler for it is an empty stub, and Author refuses to
> save such a rule. Closing the gap needs no change to the key format, the schema, the reporting
> star schema, FHHS, Admin or the `.elicit` format, and no data migration. The work is in the
> Survey runtime (section 4) and in Author (section 5). Nothing here is implemented, and the
> decisions in section 7 are open.
>
> **Scope:** count-driven repetition, where a numeric answer N produces N instances of a step, as
> REPEAT already does for a section. A respondent-driven "add another" control is a different
> interaction and is not covered.
>
> **Evidence:** statements are from reading the code on `V3` (Survey, FHHS, Admin) and `main`
> (Author) unless marked *inferred*. Nothing was executed. Inferred items are listed for
> confirmation by test in section 8.

## 1. The key already has the slot

`Survey/src/main/java/com/elicitsoftware/DisplayKey.java:197` builds the key as seven four-digit,
zero-padded segments joined by hyphens:

```
survey - step - stepInstance - section - sectionInstance - question - questionInstance
0001   - 0004 - 0000         - 0006    - 0002            - 0003     - 0000
```

| Segment | Holds |
| --- | --- |
| 1 | `surveys.id` |
| 2, 4, 6 | Display orders of the step, section and question. Not ids. |
| 3, 5, 7 | Instance counters. `0000` means "not repeated". |

- The column is `display_key varchar(34) NOT NULL` with `UNIQUE (respondent_id, display_key)`
  (`Survey/src/main/resources/db/migration/V001__Create_Survey_Schema.sql:678,685`). 34 is
  exactly 7 × 4 + 6, so the format is full: an eighth segment would need 39 characters.
- `answers` also stores the parts as columns, including `step_instance integer NOT NULL DEFAULT 0`
  (`V001:668`). `surveyreport.fact_sections` has the same column (`V002:90`).
- Survey's own entity model already describes it: `stepInstance` is "Which repeated instance of
  the step this answer belongs to" (`Survey/docs/entity_model.md:222`), and a step "may repeat as
  a unit" (`:107`). Author's says `survey.step.step_instance.section.section_instance`
  (`Author/docs/entity_model.md:248`).
- Author writes every definition key with zero instances:
  `DISPLAY_KEY_FORMAT = "%04d-%04d-0000-%04d-0000-0000-0000"`
  (`Author/.../survey/DisplayOrdering.java:40`). That is correct and stays: instances belong to a
  respondent's answers, never to the definition.

The V2 and V3 migration tracks use the same format, and no migration in any module has ever
rewritten a `display_key`. The V2 database copy already holds keys with non-zero step instances.

## 2. What exists today

### 2.1 Three actions, and no "hide"

`survey.action_types` holds SHOW (1), REPEAT (2) and TEXT (3)
(`Survey/.../db/migration/V003__Populate_Schema.sql:13-19`). There is no HIDE: anything a rule
names as its downstream is left out of the answers built at login and appears only when the rule
fires (`QuestionManager.java:640-643`).

### 2.2 REPEAT on a question and on a section

`QuestionManager.buildDownstreamQuestions` dispatches on the most specific downstream column that
is set: question, then section, then step (`:845-860`).

- **Question** (`buildRepeatedAnswers`, `:1305-1356`): one answer row per instance, numbered
  `1..N` in segment 7.
- **Section** (`buildRepeatedSections`, `:1368-1403`): for each instance a section marker row
  (no question, `display_text` = the section name) and the section's initial questions, numbered
  `1..N` in segment 5 (`setSectionInstance(i + 1)`, `:1396`).

In both, N is the upstream answer's text value parsed as an integer.

### 2.3 Step instances already work, through SHOW

A SHOW rule whose downstream is a step alone sets the step instance to the **question instance of
the answer that fired it** (`:836-841`, and `buildDisplayKey` `:1043-1050`). It then saves a step
marker row and builds every section in the step under that instance (`buildStepAnswer`
`:753-786`, `buildInitialStepAnswers` `:1217-1226`).

So "repeat the name question, then show a step per name" yields one step instance per name.
`QuestionManagerStepIdOrderTest.java:169-218` asserts exactly that: a count of 2 and the names
Alice and Bob produce step markers `…-0002-0001-…` and `…-0002-0002-…` with section titles
"About Alice" and "About Bob".

This is what the Family History Survey does. Its four REPEAT rules all target a name question
(children, siblings, mother's siblings, father's siblings), and SHOW rules then reveal a step per
name. FHHS groups relatives by exactly this: `fact.step + fact.stepInstance`
(`FHHS/src/main/java/com/elicitsoftware/model/FamilyManager.java:356`).

### 2.4 The stub

```java
// QuestionManager.java:1414-1418
private void buildRepeatedStep(Answer upstreamAnswer, Step downstreamStep, Integer relationshipId,
                               HashMap<Integer, Dependent> dependents) {
    // TODO
    Log.info("buildRepeatedStep not yet implemented");
}
```

A test pins the stub: `QuestionManagerBranchCoverageTest.java:202`,
`repeatStepOnlyBranch_isCurrentlyANoOp`.

### 2.5 Author refuses the rule

- `RuleService.validate` throws `error.rule.repeatNotStep`, "Repeat applies to a section or a
  question, not a step" (`Author/.../survey/RuleService.java:245-246`).
- The rule dialog disables the step target when the action is REPEAT
  (`Author/.../flow/designer/RelationshipDialog.java:359`).
- The spec gives the reason: "The system refuses: the runtime repeats sections and questions, not
  steps" (`Author/docs/use_cases/UC-019-create-conditional-relationship.md:51-56`, A3).
- It is already planned scope: FR-017 "Configure Repeating Elements" covers "a step, section, or
  question" and is **Open** (`Author/docs/requirements.md:61`); UC-017 is in the diagram with no
  specification written (`Author/docs/use_cases.puml:36,190`).

Neither Author's export validation nor Admin's importer checks a REPEAT rule's target, so a
hand-written `.elicit` file with a step REPEAT imports today and then does nothing.

### 2.6 The documents disagree

Survey's FR-005 lists repeating "questions, sections, or steps" as **Implemented**
(`Survey/docs/requirements.md:17`), while UC-002 A3 describes only "the configured downstream
question or section" (`Survey/docs/use_cases/UC-002-answer-survey-questions.md:40`).
`docs/research/faceted_exploration.md:188-191` (guideline G2) also assumes it: "Repeated entities
use the step's REPEAT, so `step_instance` numbers them". Today that numbering comes only from the
SHOW pattern.

## 3. What a step REPEAT has to do

For a rule with action REPEAT and only `downstream_step_id` set, fired by a numeric answer N:

1. **Build.** For each instance `i`, take the downstream step's display order, set step instance
   `i`, save the step marker, then build the step's sections. These are the same two calls the
   SHOW branch makes at `:840-841`; only the source of the instance number differs (a loop
   counter instead of the upstream's question instance).
2. **Lower the count.** Remove the instances above the new count, and only those.
3. **Raise it again.** `saveAnswer` finds an existing row by key and undeletes it
   (`:1594-1616`), so a restored instance comes back with the answers it had. This is how
   repeated sections behave today.
4. **Number the text.** `{S#}` already renders the step instance in step, section and question
   text (`:1463-1464`, `:1477-1478`), so "Sibling {S#}" needs nothing new.

Two constraints hold without new work:

- **Step 1 cannot repeat**, and no step can repeat itself: the count question has to sit in an
  earlier step. Author's ordering check already refuses a downstream step that does not come
  after the upstream question, for any action (`Author/.../survey/RuleOrdering.java:157-159`,
  `validation.rule.backwards.step`).
- **The target is hidden at login** because it is a rule's downstream step (section 2.1).

## 4. Gaps in Survey

All in `Survey/src/main/java/com/elicitsoftware/QuestionManager.java` unless noted.

| # | Gap | Where | Basis |
| --- | --- | --- | --- |
| S1 | `buildRepeatedStep` is empty. | `:1414-1418` | Read |
| S2 | Lowering the count throws. The REPEAT delete branch treats everything that is not a question repeat as a section repeat and calls `StepsSections.findAsOf(downstreamSsId)`. For a step rule that id is null, `findAsOf` returns null (`model/StepsSections.java:191-194`), and `.getKey()` fails. | `:1816-1826` | Read |
| S3 | Removing one step instance reaches into the others. `deleteStepAnswers` matches `survey-step-%`, every instance of the step, and deletes what each of those answers showed. A new instance-scoped pattern (`survey-step-instance-%`) is needed beside `getStepQueryString`. | `:1927-1932`, `DisplayKey.java:284-286` | Read; the effect is *inferred* |
| S4 | A step that has both a SHOW and a REPEAT rule from the same upstream step would have both evaluated against the one answer that fired, and both would build it with different instance numbers. | `:1978-1990` | Read |
| S5 | `getStepByDisplayKey` given a section-level key with step instance ≥ 1 finds nothing: it searches definition keys, whose instances are always `0000`, with a pattern that includes the instance. Step-level keys are unaffected. `getSectionByDisplayKey` avoids this by zeroing the instance first. | `:424-425`, `:397` | Read |
| S6 | No guard on the count: non-numeric text throws from `Integer.parseInt`, and a count above 9999 overflows a four-digit segment. True of question and section repeats too. | `:1327`, `:1391` | Read |

S3 matters beyond this feature. *Inferred:* it already affects the SHOW pattern, so removing one
sibling in the Family History Survey may delete follow-up questions that were shown inside the
other siblings' steps. The base answers of the other instances are not touched; what is lost is
whatever those answers revealed. This needs a test before it is called a bug.

Two findings that turned out **not** to be gaps:

- TEXT rules on a repeated step attach to every instance. They are found by step, not by
  instance, when the display text is built (`findRelationshipsByDownstreamAnswer`, `:1662-1699`).
  The instance pairing at `:776` only serves the SHOW pattern's per-name values.
- The initial questions of a step instance are selected without an "already answered" filter
  (`:549-574`), so instance 2 gets the same questions as instance 1.

### 4.1 Existing repeat bugs found on the way

These are in code the step repeat will sit beside. Today they cancel out, because `saveAnswer`
is idempotent by key.

- `DisplayKey.getAnswerQueryString()` ends in `".%"` (`DisplayKey.java:214`). Keys contain no
  dot, so the `LIKE` never matches and "how many instances exist already" is always zero.
- `buildRepeatedSections` passes the **relationship id** where the respondent id is expected
  (`:1388`, against `Answer.findBySectionInstancesQueryString(int respondentId, …)`).
- `{S#}` is the **step** instance, and there is no section-instance counter. The Library test
  survey names its repeated sections `Checkout {S#}` and `Renewal {S#}`
  (`Survey/src/test/resources/db/test/V9005__Library_Test.sql:236,242`), which for a section
  repeat renders the step instance, 0. *Inferred from the code; not run.*

## 5. Gaps in Author

- **Lift the refusal** in `RuleService.validate` (`:245-247`) and the dialog (`:359`), and retire
  `error.rule.repeatNotStep` from the English bundle, `translations.context.properties`, both
  translated bundles and the regenerated `i18n/TRANSLATION_REQUEST.md`.
- **One building rule per repeated step** (S4): refuse a step that is the downstream of a REPEAT
  and of another SHOW or REPEAT.
- **Nested repeats** (decision D-2), if they are to be refused.
- **Wording.** `validation.step.noBuildingRule` tells the author to "Add a Show rule on the step"
  (`translations.properties:109`); `guideView.rules.repeat.desc` and
  `relationshipDialog.help.action` describe REPEAT as acting on "the target section or question".
- **AI authoring spec.** `docs/ai/AUTHORING_FROM_AI.md:52-53` says the same and is gated against
  the code by `AuthoringFromAiDocTest`.
- **Preview.** The designer annotates only SHOW targets (`SurveyDesignerView.java:723-728`). A
  repeated step deserves the same note.
- **Specs first.** Rewrite UC-019 A3, and specify FR-017 / UC-017 or fold step repeat into
  UC-019 and narrow FR-017.
- **Tests.** `RuleServiceTest.java:132-135` asserts the refusal and flips.

The `.elicit` format does not change: a step REPEAT is `action_id = 2` with only
`downstream_step_id` set, which the format and Admin's importer already carry.

## 6. What does not change

| Area | Why |
| --- | --- |
| Key format, `varchar(34)`, constraints | The slot exists (section 1). |
| Existing data, brownfield upgrade | No key is rewritten. |
| Reporting ETL | It never parses `display_key`; it reads the instance columns. The `fact_sections` grain is already respondent × step × step instance × section × section instance (`Survey/.../etl/Sql.java:282-325`). |
| FHHS | Groups by step and step instance already. Its survey keeps the SHOW pattern unless re-authored. |
| Pedigree | Receives sequential ids; no instance numbers. |
| Admin | Rebases only the first key segment on import; exports and imports instances as they are. |
| Navigation and review | Both order section markers by `display_key`, so step instances fall in sequence. |

## 7. Decisions to make

**D-1. Instance numbering: from 1 or from 0?** The request described "Step 2 instance 0, Step 2
instance 1". Every existing repeat numbers from 1 and reserves 0 for "not repeated": question
repeats, section repeats (`i + 1`), the SHOW pattern, FHHS's tests (`stepInstance("1")`).
Numbering from 0 would make the first instance of a repeated step indistinguishable from an
unrepeated step, and "Sibling {S#}" would read "Sibling 0". **Recommendation: 1..N.**

**D-2. A count question inside a repeated step.** One slot means step repeats cannot nest.
Example: step 2 "Child" has three instances, and each asks "How many children does this child
have?", which repeats step 3 "Grandchild". Child 1 answers 2 and gets `0001-0003-0001-…` and
`0001-0003-0002-…`. Child 2 answers 1 and gets `0001-0003-0001-…`: the same key, so the same
row. Child 2's first grandchild *is* child 1's, and lowering either count deletes the other's.
The same collision exists today for a SHOW on a step fired from inside a repeated step. Options:
refuse it in Author, or leave it unguarded and documented. A repeated **section** inside a
repeated step is unaffected (grandchildren as a section of "Child" give
`step instance → section instance → question instance`, three levels). **Recommendation: refuse
in Author.** Widening the key would touch the column, every key helper, Author's writer, Admin's
import and export, every fixture with a literal key, and existing answers on upgraded databases.

**D-3. A step with both a SHOW and a REPEAT rule (S4).** **Recommendation: refuse in Author;** a
repeated step has exactly one rule that builds it.

**D-4. Fix S3 and the bugs in 4.1 with this work, or separately?** S3 is required for the
feature. The others are independent. **Recommendation: S3 here; the two cancelling bugs as their
own Survey issue, since fixing them changes how existing repeats count instances.**

**D-5. Maximum count (S6).** A ceiling protects the segment width and the respondent from a
mistyped 500. The upstream question's `max_value` may be enough if Author requires one on a
count question.

**D-6. A section-instance counter.** Whether to add one (for example `{N#}`) so a repeated
section can number itself, given `{S#}` is the step's.

## 8. Plan

AIUP order in each module: requirement, use case, then code citing the UC and BR ids.

**Survey** (branch off `V3`)

1. Correct FR-005's status; extend UC-002 A3 to steps, with business rules for numbering (D-1),
   lowering and restoring the count.
2. Implement `buildRepeatedStep` on the SHOW branch's two calls (S1).
3. Add the step branch to the REPEAT delete path (S2) and an instance-scoped step pattern (S3).
4. Guard the count (S6, D-5). Fix S5 if anything beyond `toString` reads `NavResponse.step`.
5. Tests, each citing UC-002: replace `repeatStepOnlyBranch_isCurrentlyANoOp`; add a fixture
   modeled on `QuestionManagerStepIdOrderTest` covering build, lower, raise, `{S#}`, a TEXT rule
   across steps, and **a follow-up question inside instance 1 surviving the removal of
   instance 2**. That last case also settles the inferred S3 for the SHOW pattern.

**Author** (branch off `main`)

1. Rewrite UC-019 A3; specify FR-017 / UC-017; record D-2 and D-3 as business rules.
2. Lift the refusal; add the D-2 and D-3 checks to `RuleService.validate` and export validation.
3. Update the bundles, the guide text, the preview note and `AUTHORING_FROM_AI.md`.
4. Flip `RuleServiceTest`; keep `AuthoringFromAiDocTest` green.

**Umbrella**

- Correct `faceted_exploration.md` G2 once step REPEAT exists, or note the SHOW pattern there now.
- The comment in `Author/e2e_multisite/.../HouseholdSurvey.java:14` ("the Survey runtime cannot
  repeat a whole step") goes stale; the suite itself can stay on the SHOW pattern.

**Verification**

- Greenfield: author a two-step survey with a count question and a step REPEAT, export, apply in
  Admin, answer it, lower and raise the count, and confirm `fact_sections` has one row per step
  instance.
- Brownfield (`resetDatabase.sh V2`): run the Family History Survey end to end. The new rule
  cannot be applied to a V2 database, but the S3 change alters deletion for the existing sibling
  pattern, which is what this run protects.
