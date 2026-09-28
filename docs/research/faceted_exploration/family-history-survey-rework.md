# Family History Survey — reporting layer reworked under the faceting guidelines

`family-history-survey.elicit` here is a copy of `FHHS/family-history-survey.elicit`
(2026-09-27 export) with **only its reporting layer changed**: `dimensions`, `ontology`,
`metadata`, and the `dimension_name` of steps and sections. No question, section, step, select
item or rule is touched, so the instrument a respondent sees is identical. It is a research
artifact for `../faceted_exploration.md`, not a replacement for the FHHS file: applying it to a
site relabels history (Author C-011/C-012) and renames one fact column (`other_age_key` →
`other_cancer_age_key`). `samples/validate-elicit.py` passes on it.

Rule numbers refer to section 4 of the research document.

## What changed

| # | Change | Rule | Effect in a recordset |
|---|---|---|---|
| 1 | `Relationship` constants `0 / 1 / 2` → `Proband / First degree / Second degree` (10 assignments) | G4 | the Relationship tab reads as words |
| 2 | `Generation` constants `-2 / -1 / 0` → `Grandparent / Parent / Proband generation` (11 assignments) | G4 | the Generation tab reads as words |
| 3 | `Generation = Child` added on the *Children* step's first section (metadata 100) | G3 | children no longer show as "(unanswered)" on Generation |
| 4 | `Generation = 1` **removed** from the *Siblings* step's Cancers section (metadata 7) | G3 | siblings are the proband's generation; a constant on the second section only reached the cancer row |
| 5 | `Race` re-attached from question 52 (free text "Please specify other race?") to question 51 (the race MULTI_SELECT) (metadata 98) | G4 / bug | `dim_race` fills; see the G6 caveat below |
| 6 | New tag `Cancer Status` on question 7 ("Do you currently have, or have you ever had cancer?", RADIO Yes/No/unknown), question scope (ontology 69, metadata 101) | G5, G14 | an *Any cancer* facet on every relative and the proband, without deriving it from 19 flags |
| 7 | New tag `Filled By` on question 2 ("Are you … the patient?", RADIO), question scope (ontology 70, metadata 102) | G18 | a respondent-level *patient / not patient* facet on the Demographics row |
| 8 | Tags retitled: `Kidney renal cell Cancer` → `Kidney Renal Cell Cancer` (and its Age / Multiple), `Non-Melanoma Skin Cancer` → `Nonmelanoma Skin Cancer` (and its Age / Multiple), `Other Age` → `Other Cancer Age` | G10 | tab labels in one style; the hyphen no longer differs between table and column name; only `Other Cancer Age` changes a column |
| 9 | Step `dimension_name`: `Welcome / Demographics / Thank you` → `FHHS Welcome / FHHS Demographics / FHHS Thank You`; section `dimension_name`: `Introduction / Demographics / Cancers` → `FHHS …` | G15 | no `dim_step_un` / `dim_section_un` collision with another survey's `Welcome` on the same site (the failure the proof of concept hit); relative steps keep their names because they are the *Relative* facet |
| 10 | Section 9 `dimension_name` `" Maternal Grandfather"` → `"Maternal Grandfather"` (leading space) | hygiene | one fewer accidental value |

Header counts updated: `ontology: 70`, `metadata: 101`.

## What was deliberately not changed

- **Cancer flags stay answer-valued (`true` / `false`).** A constant `Yes` on a CHECKBOX would
  also fire for a box that was checked and then unchecked (`text_value = 'false'`), so the answer
  path is the only correct one. "No cancer" is the absence of the row or `false`, which the
  Diagnoses recordset filters on `= 'true'`.
- **`Race` is a MULTI_SELECT**, which G6 says not to tag for its answer (comma-joined values in
  unstable order). Re-attaching the tag fixes the outright bug; the combinatorial values remain
  until E6 (multi-select split) or the question becomes one CHECKBOX per race. Left as the
  documented exception.
- **The proband's diagnoses are a separate step (*Proband Cancer*)**, which G2 says not to do;
  fixing it means moving a section between steps and rewriting the rules that show it. Left to
  E3 (`steps.entity`) to declare the two steps one entity.
- **No age band** (G13) — it needs a new RADIO question or a banded dimension (E1).
- **`Age` stays one tag fed by two questions.** "What is X's current age?" and "What was X's age
  at death?" both report to `Age`, with `Vital Status = Deceased` carried by the second: a relative
  has one age, and the facet is *Age* with *Vital Status* beside it, not two half-empty tabs. The
  diagnosis ages are a different *role* of the same `age` table and stay separate tags. This is
  the role-playing rule of section 4 (G11): the dimension is the value table, the tag is the role,
  and a role may collect several questions.
- `Vital Status = Alive` is carried twice on the proband (on the section and on the age
  question); harmless, left.
- Dimensions are unchanged: `age`, `cancer` and `multiple_cancers` already follow G11
  (one per domain); `age` is the model case — one integer table shared by 20 tags, which is
  where E1's `kind = integer` and its bands would be declared once for all of them. `Cancer Status` and `Triple Negative Breast Cancer` share the
  Yes/No/unknown vocabulary but different meanings, so they keep their own tables (G9).

## Recordsets this definition yields

With the reworked tags the three recordsets of the research document need no label mapping:

- **Relatives** — tabs: Relative (step), Relationship, Generation, Gender, Vital Status, Cancer
  Status, Age, Ashkenazi, LatinX, Race; results: the 19 × 3 site columns, Sibling Type, Shared
  Parent, Filled By (via the respondent), respondent id, instance.
- **Cancer diagnoses** — tabs: Relative, Relationship, Generation, Gender, Cancer site, Age at
  diagnosis, Multiple, Triple negative; `--key Respondent` (or a respondent-plus-relative key for
  "this person has both").
- **Respondents** — `fact_respondents` facets plus Filled By.
