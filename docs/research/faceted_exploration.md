# Authoring Surveys for Faceted Exploration: Ontology, Metadata and BinaryFilter

> **Status (2026-09-28):** Research. A proof of concept (section 3) showed that the reporting
> star schema Survey's ETL builds from a survey's reporting tags converts into BinaryFilter
> recordsets that facet well — once the SQL compensates for what the tags do not say. This
> document turns that experience into **guidelines for authoring new surveys** (section 4) and
> **recommendations for the platform** (section 5) so that the compensation disappears and a
> survey's recordsets can be derived from its metadata. Two directions were decided while
> writing it: **every survey gets its own reporting tables — dimensions are not shared between
> surveys — and the `survey_id = 1` hard-coding in Survey's ETL is removed.** Where to host the
> browser (section 6) is deliberately not decided. Nothing here is implemented.
>
> The proof of concept's SQL, generator and engine check are under `faceted_exploration/` so it
> can be repeated (section 7).

## Overview

[BinaryFilter](../../../Source/BinaryFilter) (`../Source/BinaryFilter`; modules `engine`, `sql`,
`producer`, `viewer`, `web`) turns any rectangular result set into a faceted browser: every
column becomes a tab listing its distinct values with live counts, the last tab lists the rows,
and each selection narrows every other tab to the values still reachable. Its input is a
`.bfilter` file — one sorted value dictionary per column and one bit-packed code per cell —
written once from a CSV or a JDBC query.

Elicit's reporting schema is a Kimball star that Survey's ETL builds from a survey's **reporting
tags**: narrow `dim_<tag>(id, value)` tables, one integer `<tag>_key` per tag on `fact_sections`,
and two denormalized views. Structurally that is exactly what a faceted engine builds for itself
from a flat table (section 1.1), so the two fit. What decides whether the result is *useful* is
what the tags say: which questions are worth a tab, what a value is called, which tags describe
the same thing, and which groups of tags repeat. Today an author can express some of that and
the rest is implied by tag names — which is why the proof of concept needed hand-written SQL.

Terminology: a **recordset** is one SQL statement whose result becomes one `.bfilter` file
(BinaryFilter's own term); a **facet** is a tab; an **entity** is what one recordset row
describes (a household member, a diagnosis, a respondent); a **dimension** is a value
vocabulary shared by several tags; a **tag** is one column of the report.

## 1. What a faceted browser needs from a source

Everything in this section is BinaryFilter's own documented behavior (`docs/vision.md`,
`docs/requirements.md`, `docs/filter_file_format.md`, ADRs 0009, 0010, 0011, 0014,
`docs/benchmarks.md`).

### 1.1 Shape

- **One rectangular table, one header row, exactly one value per cell.** "Every row has exactly
  one code in every column." A comma-joined cell is one value whose text contains commas: a tab
  of combinations, not of values.
- **Every column is categorical**, numbers and dates included. A column's declared type changes
  only how its values are ordered and aligned — never what a value is or which rows carry it.
- **The column alias is the tab label** (`ResultSetMetaData.getColumnLabel`); labels must be
  non-empty and unique.
- **Leading columns are tabs, in column order; the rest are results-only** (`--tabs n`). Put the
  facets first in the order you want them; push ids, links, free text and near-unique columns to
  the right.
- **Row order is the code order.** The query needs a deterministic `ORDER BY` ending on a key or
  the same query converts to different bytes on two runs; the ordering costs almost nothing
  ("a normalized source costs 22 %").

### 1.2 Values

- **Types are declared, never inferred, on the JDBC path**: integer, decimal, boolean, date,
  time, timestamp, timestamptz, uuid, else text. A value that does not parse under its declared
  type refuses the whole file. Cast in the query (`::numeric`, `::date`); enums arrive as text.
- **Null and empty are different values** — shown as `(null)` and `(empty)` — and **a column may
  hold one or the other, never both**; a database column holding both is refused (`coalesce` in
  the query). For a survey: *unanswered* should be the null value, consistently.
- **Case and whitespace are significant**; `Female` and `female` are two values.

### 1.3 Cost

- A column's distinct-value count sets its code width (`ceil(log2(values))` bits per row) and
  its dictionary size, and the sort of one near-unique column dominates conversion. A
  **results-only** high-cardinality column costs no counts and no per-session memory; a tab with
  thousands of values works but is navigated by the find field, not by scrolling.
- Reference: 1.5 M rows × 14 tabs converts in ~10 s of a 30 s budget, opens in 0.6 s, and the
  worst click is 200 ms server-side; 14 tabs redrawn at once is the one measured latency miss.
  **Ten or fewer well-chosen tabs, each with a handful to a few dozen values, is the sweet spot.**

### 1.4 Things that span rows: the record key

ADR 0014 adds a **record key** (`bfilter --key <column>`): a column that says which rows are one
record. With it, a tab can be switched from *Or* to *And* — "records with breast cancer **and**
endometrial cancer" on a one-row-per-diagnosis table — and the key column's own tab shows no
choice. This is the intended way to expose repeating groups: **unpivot to one row per value and
name the key**, rather than multi-valued cells. The preparer chooses the key's grain ("the
person themselves has both" needs a respondent-plus-relative key; "anywhere in the family" needs
the respondent). Counts on non-key tabs are counts of rows, not of records.

### 1.5 The Data Explorer

The web viewer opens a named statement against an operator-declared connection
(`binaryfilter.source.<name>.statement.<key>`), with a tab count and a record key, bounded by
`timeout` (300 s), `max-rows` (5,000,000) and `conversions` (2) — and caches nothing, so counts
never go stale. A survey's recordsets are exactly such named statements.

## 2. What the reporting model can say today

### 2.1 What an author controls (the authoring tool's Reporting page, its own UC-021–UC-024)

1. **Tag name** (`ontology.tag`, letters/digits/spaces/hyphens, effectively ≤ 59 characters so the
   derived column stays under PostgreSQL's 63) → the `fact_sections` column
   `lower(replace(replace(tag,' ','_'),'-','')) || '_key'`.
2. **Dimension, or none** (`ontology.dimension`) → several tags on one dimension resolve their
   values in one `dim_<lower(name)>` table; a tag without one gets `dim_<lower(replace(tag,' ','_'))>`
   (hyphens kept, hence forbidden). Dimensions are matched by name case-insensitively and are
   **platform-wide** today (UC-023 BR-001) — the thing section 5.1 changes.
3. **Attachment point**, fixed once saved: the question wherever it appears (`question_id`), the
   question in one section only (`sections_question_id`), or a section as mounted in one step
   (`steps_sections_id`). Steps cannot be tagged.
4. **Reported value**: `metadata.value` `NULL` = the respondent's answer; a constant = that value
   for every respondent who answers anything in the element (the `Gender = Female` on a Mother
   section pattern).
5. **Step and section `dimension_name`** (≤ 50 characters, unique site-wide today) → the values of
   `dim_step` / `dim_section`.

Author's impact preview (UC-024) already derives the dimension tables and columns exactly as the
ETL will, and blocks export on invalid identifiers, two tags on one column, and a name outside
`^[a-z0-9_]+$`.

### 2.2 How a value is derived (`Survey/.../etl/Sql.java`)

`lower(trim(metadata.value))` when the constant is set, else
`lower(trim(answers.text_value))`; empty values are dropped; every dimension carries a `(-1, NULL)`
"no value" member that every fact column defaults to. `text_value` is `select_items.coded_value`
for a RADIO/COMBOBOX, the literal `true`/`false` for a CHECKBOX, the **comma-joined coded values
in unstable order** for a MULTI_SELECT / CHECKBOX_GROUP, and free text otherwise.
`select_groups.data_type` and `questions.data_type` never reach the ETL. Dimension values are
`varchar(255)`.

### 2.3 What cannot be expressed

A dimension member's **type**, **order**, **display label** or translation (C-024: tags and
dimension names are identifiers, never translated); any **grouping or hierarchy**; the **entity**
a tag describes (respondent vs. mother vs. child instance — only mimicked with constants on
sections); which tags are **facets** and in what order; **measures**; splitting a
**multi-select**; tagging a **step**. The published guidance is one paragraph of the author's
manual (§8 "Tagging for reporting") plus "use `lower_snake_case` for `dimension_name`", and the
AI-authoring spec explicitly leaves reporting to a person.

## 3. Evidence: the proof of concept

Run on the dev stack against the Family Health History Survey with 3,000 synthetic finalized
respondents generated straight into `survey.answers` (171 k answers, 36 s), back-filled by
`POST /api/etl/build` (319 s, 55 k fact rows), converted by the `bfilter` producer over three
hand-written recordsets, and checked through the engine API against SQL oracles:

| Recordset | Rows / tabs | Convert | Checks (engine = SQL) |
|---|---|---|---|
| Relatives — one row per reported relative | 46,014 / 10 | 3.0 s, 1.35 MB | Mother → Gender collapses to `female` (2,984 + 3 unanswered); + deceased = 1,042; 6 ms + 4 ms |
| Cancer diagnoses — one row per relative × site | 16,694 / 7 | 2.7 s | Mother = 1,125 over 19 sites; female + alive + Breast Cancer + age < 50 = 411; 18 ms |
| Respondents | 3,012 / 6 | 0.6 s | year → month cascade |

What the hand-written SQL had to supply, because the tags do not say it:

- **the entity**: a relative's answers sit on two `fact_sections` rows (the step-level row carries
  the demographics, the *Cancers* section row the diagnoses; the proband's diagnoses are a
  separate step), merged with `GROUP BY respondent_id, step, step_instance`;
- **the repeating group**: 19 flag tags × (age, multiple, triple-negative) unpivoted through
  `LATERAL (VALUES …)` into one *Cancer site* facet — the set of tags sharing the `cancer`
  dimension, paired with their attributes by name prefix;
- **labels**: `dim_relationship` = `0/1/2`, `dim_generation` = `-2..1`, `dim_cancer` = `true`/blank,
  because the constants were written as codes;
- **types**: `::numeric` on ages; `nullif(col, '')` so unanswered is the null value;
- **the facet list and order**, and the 57 columns that must be results-only.

It also exposed schema and ETL facts recorded in section 5 (hard-coded survey, site-wide
dimension names, a mis-attached `Race` tag, a missing `Generation` constant on *Child*).

`faceted_exploration/family-history-survey.elicit` is a copy of the FHHS definition with its
reporting layer reworked under section 4's rules — constants relabeled, the missing and stray
`Generation` constants fixed, `Race` re-attached, a `Cancer Status` and a `Filled By` tag added,
tab labels harmonized, non-entity `dimension_name`s prefixed — and nothing else changed;
`family-history-survey-rework.md` beside it lists every change, what was deliberately left (the
MULTI_SELECT race, the split proband entity, no age band) and the recordsets that result.

## 4. Guidelines for authoring a new survey

Each rule says what to do, why, and whether it is enforceable today (**T**) or needs the
platform changes of section 5 (**P**).

### 4.1 Decide the entities first

- **G1 — Name the entities before the tags.** List what a recordset row will describe: the
  respondent, and each repeating thing (household member, medication, episode). Every tag then
  belongs to exactly one entity. *Why:* the merge grain in section 3 is the single most expensive
  thing to reconstruct after the fact. **T** (as a design step) / **P** (as `steps.entity`, E3).
- **G2 — One entity per step; repeat by step instance.** Put everything about one entity in one
  step (its own sections are fine); never split an entity across steps. Repeated entities use the
  step's REPEAT, so `step_instance` numbers them. *Why:* `fact_sections` rows already carry
  `(step, step_instance)`, so an entity that is one step is one `GROUP BY`. **T**.
- **G3 — Entity constants on the first section, complete.** Tag the entity step's first section
  with the fixed values that identify the entity (`Relationship = Mother`, `Generation = Parent`,
  `Gender = Female` where fixed) and put no entity-level constant on any later section. *Why:* a
  constant on a later section only appears on that section's row. **T**.

### 4.2 Make values facet-ready

- **G4 — Values are labels, never codes.** `select_items.coded_value` is what is reported, so it
  must be human text (`Deceased`, not `2`); a constant `metadata.value` likewise. *Why:* the facet
  shows the stored value verbatim (lowercased); there is no label layer. **T**.
- **G5 — Keep vocabularies small and closed.** Prefer RADIO / COMBOBOX (one coded value) and
  CHECKBOX (`true`/`false`) for anything that should be a facet. Numbers that people filter by
  range (age, count) will need banding (G13). *Why:* a tab is a list; a facet with hundreds of
  values is navigated by find, not by browsing. **T**.
- **G6 — Do not tag a MULTI_SELECT or CHECKBOX_GROUP for its answer.** The comma-joined string
  becomes one dimension member per *combination*, in unstable order. Model a multi-choice as one
  CHECKBOX per option (each a boolean tag), as a repeating entity, or — the third way, since
  2026-10-02 — repeat a *section* once per selected item: each instance is then one `fact_sections`
  row carrying the item in `item_key` / `fact_sections_view.item` and the question in
  `question_key` (Survey UC-008 BR-012). A repeated
  *question* gets no such row. *Why:* section 2.2. **T** (until E6 splits the answer itself).
- **G7 — Never tag free text, dates, email or password questions for their answer.** Free text
  is unbounded (and fails the insert past 255 characters); a password would land a secret in a
  reporting table. Use a constant if the *fact of answering* matters. **T**.
- **G8 — `metadata.value` is `NULL` for "use the answer"; never an empty string.** The live
  queries treat `''` as a constant that is then dropped. **T**.
- **G9 — Do not overload a dimension with two meanings.** A `yes_no` dimension is fine for many
  yes/no tags; `status` meaning one thing for medications and another for appointments is not,
  even if the values look alike. *Why:* a shared dimension is one value space. **T**.

### 4.3 Name and group tags so recordsets can be derived

- **G10 — Tag text is the tab label.** Title Case, no abbreviations, no hyphens (a dimension-less
  hyphenated tag is an invalid table name; a hyphen vanishes from the column name so
  `Risk-Score`, `Risk Score` and `RiskScore` collide). Keep tags under ~55 characters. **T**.
- **G11 — One dimension per domain, role-played by tags.** A dimension is a value table —
  `age` (integers), `date` (the calendar), `yes_no`, `vital_status`, `gender` — named for the
  domain, not for a question. A **tag is a role** of that table and becomes one tab: `Age` and
  `Breast Cancer Age` are two roles of `age`; created / first access / finalized are three roles of
  `dim_date`. **A role may be fed by several questions when they are the same fact under
  different conditions**: a relative's *current age* and *age at death* both report to `Age`
  (with Vital Status beside it), because a relative has one age; a diagnosis age is a different
  role and its own tag. *Why:* comparable values across roles, one value dictionary, and (after
  E1) one place to declare the type and bands for every role at once. **T**.
- **G12 — Repeating groups: `<Item> <Attribute>` naming, one flag per item.** For a group such as
  diagnoses or medications, one boolean tag per item (`Breast Cancer`, `Lung Cancer` on a `cancer`
  dimension) and its attributes as `<Item> Age`, `<Item> Multiple` on domain dimensions. *Why:*
  the unpivot of section 3 pairs them by this prefix until E2 declares the group. **T** now,
  **P** to make it declared rather than inferred.
- **G13 — Numeric facets need a band.** Until E1, add a second RADIO question (or a section
  constant) for the band people actually filter by (`Under 40`, `40–64`, `65+`) alongside the
  integer, and tag the band. **T** / **P**.
- **G14 — Decide the facets and their order up front.** Aim for six to ten tabs per recordset.
  Everything else is a results-only column. *Why:* section 1.3. **T** (as a convention in the
  recordset) / **P** (E4).

### 4.4 Hygiene the platform does not enforce yet

- **G15 — Step and section `dimension_name`s are ≤ 50 characters and unique across the site**
  until section 5.1 lands; prefix them with the survey's short name (`fhhs_welcome`) if another
  survey may share a name. **T**.
- **G16 — Retired tags keep their column.** Nothing reclaims a `fact_sections` column, and
  PostgreSQL caps a table at 1,600 columns site-wide. Reuse a tag rather than mint a variant.
  **T**.
- **G17 — Tag changes are retroactive.** Changing or removing an assigned tag relabels answers
  already recorded at deployed sites (Author warns twice). Design the tags with the survey, not
  after the first respondents. **T**.

### 4.5 The respondent recordset

- **G18 — Respondent-level facets come from `fact_respondents`, not from tags.** Status, dates
  (year / month / weekday through `dim_date`), logins, duration band, and — once E5 exists —
  department/site. Tag only what varies per respondent *answer*; a respondent's identity stays in
  `survey.subjects` and out of every recordset. **T** / **P**.

### 4.6 Worked example: a new household survey under the guidelines

`samples/census-household-survey.elicit` (generated by `samples/generate-census-household-survey.py`)
is tagged to these rules and is the concrete reference: dimensions `age` and `gender`, each
role-played by a respondent tag and a household-member tag (`Age` / `Member Age`,
`Gender` / `Member Gender`); `Marital Status`, `Tenure`, `Subsidized`, `Vehicle Count`,
`Household Size`, `Member Relationship` on their own tables; every tag at question scope reporting
the answer; Race (CHECKBOX_GROUP) and Languages (MULTI_SELECT) deliberately untagged (G6); the
per-member step's `dimension_name` set to `Household Member` and the others prefixed `Census`
(G15). The same rules, applied by hand, tag the smaller `Admin/docs/manual/capture/fixtures/household-survey.elicit`.
The example below extends that survey with a repeating *Conditions* group to show E2.

Entities: *Respondent*; *Household member* (repeating step *Member*, sections *About them* and
*Conditions*); *Condition* (a repeating group within *Member*).

| Element | Tag → dimension | Value |
|---|---|---|
| step *Member*, section *About them* | `Relationship` → `relationship` | constant per step variant, e.g. `Partner`, `Child`, `Parent`; or the RADIO answer when the step is generic |
| RADIO "Sex assigned at birth" | `Gender` → `gender` | answer: `Female` / `Male` / `Other` |
| RADIO "Is this person living?" | `Vital Status` → `vital_status` | answer: `Alive` / `Deceased` / `Unknown` |
| INTEGER "Current age" and INTEGER "Age at death" (one shown per vital status) | both → `Age` on `age` | answer — one role, two questions |
| RADIO "Age group" | `Age Group` → `age_band` | answer |
| section *Conditions*, CHECKBOX per condition | `Diabetes`, `Asthma`, `Hypertension`, … → `condition` (boolean) | `true` when checked |
| INTEGER "Age at diagnosis" (one per condition, shared question) | `Diabetes Age`, `Asthma Age`, … → `age` | answer, tagged on the *section question* so the shared question gets the per-item tag; other roles of the same `age` table |
| RADIO "Still treated?" per condition | `Diabetes Treated`, … → `yes_no` | answer |

Recordsets that follow, with no survey-specific SQL once E1–E4 exist: **Members** (tabs:
Relationship, Gender, Vital Status, Age Group, Any condition; results: Age, the per-condition
columns, respondent, instance), **Conditions** (tabs: Relationship, Gender, Condition, Age Group
at diagnosis, Treated; key `Respondent` — or `Respondent+Member` for "this person has both"), and
**Respondents**.

## 5. Recommendations for the platform

Ordered by what each one removes from the guidelines.

### 5.1 One reporting schema per survey, and no hard-coded survey (decided)

Today one `fact_sections` holds every survey's tag columns, `dim_<tag>` is one table per tag name
site-wide, `survey.dimensions.name` is globally unique, `dim_step_un` / `dim_section_un` are
unique on `value` across surveys (the reason `POST /api/etl/build` failed on the dev database, and
why a Survey instance serving many draft surveys has to run with `elicit.etl.enabled=false`), and
the `survey.respondents` triggers
plus `Sql.INSERT_MISSING_FACT_SECTION_SQL` guard on `survey_id = 1` while
`FIND_MISSING_FACT_SECTION_RESPONDENTS` has no survey filter at all.

Proposed: one schema per survey — `report_<survey>` with its own `dim_*`, `fact_sections`,
`fact_respondents` and both views. Identifiers stay byte-identical to what `Sql.java` and
`ReportingNames` generate today (only the schema qualifier changes); uniqueness becomes
per-survey for free; grants are per schema; `survey.dimensions` becomes unique on
`(survey_id, name)`; `fact_respondents` is filled by the ETL (removing the trigger's `dim_date`
fragility); `POST /api/etl/build` takes the survey key and Admin passes the key of the survey it
just applied. *Removes G15 and the 1,600-column cliff of G16 (per survey instead of per site),
and makes "a recordset is this survey's schema" literally true.*

Open: the naming key (survey `key` is stable but unreadable; a slug assigned once at first build
and stored on `survey.surveys` gives both), and whether the existing `surveyreport` is migrated
or regenerated from `survey.answers` by the back-fill (regeneration is far simpler and must exist
for the brownfield `migration-v3` track anyway).

*Answered 2026-10-01, implemented as Survey UC-008 BR-006: a slug stored on `survey.surveys` that a
site can rename, and regeneration. Implemented in Survey on 2026-10-02 (Survey UC-008, UC-010,
UC-011; V021): the schema is `report_<slug>` on `survey.surveys.report_schema`, `surveyreport`
keeps only `dim_date` and `dim_status`, and `fact_respondents` became a view rather than an
ETL-filled table. `survey.dimensions` stays unique site-wide (that doc's Q-4).*

Consumers that move: Survey ETL and its tests; FHHS `CancerHistoryRepository` (reads
`fact_sections_view` by position) and `V0.0.6__Add_Performance_Indexes.sql`; the authoring tool's
reporting-name derivation and impact preview (separate repository); Admin
`ReportingSchemaRebuildClient` and
the grants migration; umbrella `CLAUDE.md`, `DeploymentScript.md`, the installation manual.

### 5.2 Reporting model extensions (authoring tool → `.elicit` → Survey ETL)

| | Field | Removes | What the ETL and a recordset generator do with it |
|---|---|---|---|
| E1 | `dimensions.kind ∈ {categorical, boolean, integer, decimal, date, text}` + optional `bands` | G13 | `dim_*` value typed and cast in the view; a banded companion column; `boolean` marks the flags an unpivot switches on |
| E2 | **Facet group**: `ontology.group` with one member as *discriminator* and the rest as *attributes* | G12's naming dependence | one generated recordset per group: entity facets + discriminator + attributes, one row per (entity × member where the flag is true), with the record key set |
| E3 | **Entity**: `steps.entity`, shared by every step describing the same kind of thing | G1–G2's discipline | the merge grain: all sections of one step instance, and steps sharing an entity, become one row |
| E4 | `ontology.facet_order` (null = results-only) | G14 | which tags are tabs, in which order; `--tabs` follows |
| E5 | respondent-level dimensions (department/site) on `fact_respondents` | — | every recordset sliceable per site |
| E6 | **Multi-select split**: the ETL emits one dimension member per coded value, or the group of E2. Partly met since 2026-10-02 for the questions an author chooses to repeat a section on: one fact row per selected item with `item` as its value (Survey UC-008 BR-012) | G6 | a CHECKBOX_GROUP becomes a facet group without one CHECKBOX per option |

(Section 5.5 proposes carrying E1–E4 and E6 as columns of a redesigned role table rather than
as additions to `ontology` / `dimensions` / `metadata`.) With E1–E4 a generator produces, for
any survey: one recordset per entity, one per facet group and one for respondents — the three the proof of concept wrote by hand. The
authoring tool's reporting service already predicts the schema from `ontology` and `dimensions`
and is the natural place for the fields and for a "predicted recordsets" preview beside the impact
panel. Each field travels the route the existing three do: `survey` DDL → JPA entity → reporting
validation → dialog → `.elicit` arity → the authoring tool's exporter/importer → Admin
export/import/update/apply → `Sql.java` → the AI-authoring spec → `entity_model.md`.

### 5.3 ETL corrections that guidelines currently work around

1. **Preserve label case** (or add a display value) — `Female` should not become `female` on a
   tab.
2. **Re-populate on metadata change.** The build only inserts dimension values and skips
   respondents that already have fact rows; a relabel or a re-attached tag never reaches existing
   data. With per-survey schemas, a build may drop and regenerate the survey's schema.
3. **Reclaim columns of retired tags** (G16) — trivial once the schema is per survey.
4. **Guard identifier length in Survey** — `Sql.requireValidIdentifier` checks only
   `^[a-z0-9_]+$`; the 63-character rule lives only in Author, so a definition arriving by any
   other route can silently truncate and collide.
5. **Treat `metadata.value = ''` as `NULL`** (G8) and align the three queries that disagree on it.
6. **Refuse a tag on a MULTI_SELECT / CHECKBOX_GROUP / TEXT / PASSWORD question at export**
   (Author impact errors, UC-024 BR-002) until E6 exists — the cheapest way to make G6 and G7
   enforceable.
7. **`fact_respondents.duration`** is `finalized_dt − created_dt`; first access is the intended
   start. **`status`** should reference `dim_status`.

### 5.4 Guidance where authors will see it

Section 4 belongs in the author's manual (§8 "Tagging for reporting" is one paragraph today) and
in the AI-authoring spec, which currently tells an AI author to leave reporting to a
person; with E1–E4 the reporting layer becomes specifiable enough to draft.

### 5.5 A simpler mapping model: declared roles instead of harvested tags

The three tables of section 2 do two jobs, and both have a simpler form. `dimensions` is a
value table; `ontology` is a report column (a *role*) carrying a namespace nobody reads;
`metadata` says "this element feeds that column, with this constant". Everything else the ETL
and the browser need — the column's identifier, type, ordering, labels, entity, group — is
either mangled out of the tag text at build time or harvested from whatever respondents typed,
lowercased. That is the root of the codes-instead-of-labels, comma-joined multi-selects,
vanishing hyphens and incomplete value lists that sections 3 and 4 work around. E1–E6 bolt the
missing facts onto the existing tables; this section proposes the shape those facts take when the
mapping is designed for them. It subsumes E1–E4 and E6.

**Change 1 — the select group *is* the dimension for choice answers.** A RADIO / COMBOBOX /
CHECKBOX_GROUP already carries a complete, ordered, labeled value list: `select_items.display_order`
is the sort order, `display_text` the label, `coded_value` the value. Today the ETL discards all
of it and rebuilds `dim_gender` from observed answers. Instead the dimension table is **populated
from the select items at build time** — every value present before the first respondent, in
authored order, with its label — and the fact column keys the select item. A choice question then
needs no dimension declaration at all, and a group shared by two questions ("your gender", "this
person's gender") is already the shared dimension. Numbers, dates and booleans use a small set of
**built-in typed dimensions**: `integer` (with optional bands), `date` (which `dim_date` already
is), `boolean`. Free text is never dimensioned. Consequences: no lowercasing, no incomplete or
unstable value lists, labels and translations for free (`select_items` is already translatable),
and a CHECKBOX_GROUP becomes one boolean column per item of its group — E6 without a splitter.

**Change 2 — the role is an attribute of the element, not a join table.** The three attachment
scopes are just *which row carries the attribute*:

| today (`metadata` row) | proposed |
|---|---|
| `question_id` + tag | `questions.report_role` — the question wherever it appears |
| `sections_question_id` + tag | `sections_questions.report_role` — in this section only |
| `steps_sections_id` + tag + `value` | `steps_sections.report_role` + `report_value` — a constant for the section as mounted |

`metadata` disappears. The author sees "Reports as: ___" on the element being edited, where the
consequence is visible (which is what the AI-authoring spec asks for). Two questions feeding one
role — current age and age at death → `Age`, the G11 rule — is two elements naming the same role.
What is lost is one element feeding *two* roles; FHHS uses that only for the
`Vital Status = Alive` constant on the proband's age question, which belongs on the section.

**Change 3 — the role carries the facts explicitly.** `report_roles`, per survey, replaces
`ontology`:

| column | meaning | replaces |
|---|---|---|
| `identifier` | the fact column / dimension suffix, snake_case, validated once (63-char rule included) and never re-derived from the label | `lower(replace(replace(tag,' ','_'),'-',''))` in `Sql.java` and `ReportingNames` |
| `label` | the tab title; translatable like any authored text | `ontology.tag` (C-024 "never translated") |
| `dimension` | a select group, or a built-in type (`integer`, `date`, `boolean`, `text`) | `ontology.dimension` → `dimensions` |
| `bands` | optional, for `integer` / `date` | E1 |
| `entity` | the step kind whose row this describes (`Relative`, `Respondent`) | E3 (`steps.entity`) |
| `facet_group`, `is_discriminator` | the repeating group (*Cancer diagnosis*) and which role names the item | E2 |
| `facet_order` | tab position, null = results-only | E4 |

`survey.dimensions` and the `ontology.name` namespace go away; a survey's value tables are its
select groups plus the built-ins, which is also what section 5.1's per-survey schema wants.

**What stays the same.** The star schema on the other end — dimension tables, `<identifier>_key`
columns, `fact_sections`, the views — and therefore the recordsets and the browser. The `.elicit`
transport shrinks: three tables become one `report_roles` table plus one or two attributes on
rows that already travel. Author's impact preview (UC-024) still derives the schema, from
declared identifiers instead of mangled labels. The retroactivity rules (C-011/C-012) are
unchanged: a role change still relabels history.

**Migration** is mechanical. Each `ontology` row becomes a `report_roles` row whose `identifier`
is today's derived column name (so the FHHS schema is unchanged after the move) and whose
`dimension` is the select group of the questions that feed it, or `integer` / `boolean` / `text`;
each `metadata` row becomes the attribute on its element; `survey.dimensions` rows that named a
shared vocabulary map to a select group where one exists (`gender`, `vital_status`) and to a
built-in type otherwise (`age` → `integer`, `cancer` and `multiple_cancers` → `boolean`).

**Cost.** Author's Reporting page and element dialogs, the `.elicit` format, Admin's
export/import/update/apply services, Survey's ETL and both migration tracks — all four repos.
The ETL is rewritten for section 5.1 either way, so this belongs *with* that work, not after it;
done separately it would be a second rewrite of the same code.

## 6. Where the browser lives (not decided)

- BinaryFilter's `engine` and `sql` are dependency-free library jars (already in `~/.m2`); its
  `web` module is an application (`AppShellConfigurator`, an unqualified `@Layout`, `@Route("")`,
  Lumo stylesheet with unscoped rules, no security), so embedding in Admin means reusing its
  ~700 lines of view source (`BrowseView`, `ValuesGrid`, `ResultsGrid`, `OpenDialog`, `Sources`,
  `Queries`) under Admin's `MainLayout`, `@RolesAllowed(ElicitRoles.ADMIN)` and Aura — either by
  copying, or by carving a component jar out of BinaryFilter — section 6.1 — (which has no
  license file or headers; Admin's build stamps PolyForm Noncommercial on anything copied in).
- Admin-side: a route and nav entry, a writable scratch directory (the first non-read-only mount
  in `docker-compose.yml`; umbrella C-013), `elicit.explore.*` settings the installation manual's
  property gate will require, a build step after apply, per-schema read grants, and an amendment
  to `Admin/docs/vision.md`'s "cross-survey analytics dashboards → downstream reporting tools" —
  or the observation that, per survey, this is single-survey exploration.
- The zero-code alternative: `elicitsoftware/binaryfilter-web` beside Admin with each survey's
  recordsets as named statements. It has no accounts, so it sits behind the network or a proxy.

### 6.1 Packaging the engine and the UI, and how the CSS travels

Whichever host is chosen, the shape that lets BinaryFilter be consumed unchanged is **three plain
jars, with the stylesheet inside the component jar**.

| Artifact | Contents | Status |
|---|---|---|
| `binaryfilter-engine` | `FilterFile`, `Session`, format reader/writer, CSV | a dependency-free jar today (in `~/.m2`) |
| `binaryfilter-sql` | `FilterWriter`, `JdbcSource`, dialects | a jar today; depends only on engine + `java.sql` |
| **`binaryfilter-vaadin`** (new module) | a `FacetBrowser` composite — `TabSheet`, one `ValuesGrid` and find field per tab column, `ResultsGrid` last — plus `Titles` and `Links`; **no** `@Route`, `@Layout`, `AppShellConfigurator`, CDI, configuration, `Library`, `Sources`, `Queries` or `OpenDialog` | carved out of `web`, which then depends on it |

Rules for the component jar so it drops into Admin — or any Vaadin 25 Flow application —
without adaptation:

- **Plain components, constructed with `new`.** The constructor takes a `Session` (or
  `FacetBrowser.over(FilterFile)`), the component closes the session on detach, and what the web
  viewer's `MainLayout` does today becomes API — `reset()`, `setHideUnavailable(boolean)`,
  `title()`, a `SelectionChangedEvent` — so the host supplies its own toolbar. That removes the
  `getParent().filter(MainLayout.class::isInstance)` coupling `BrowseView` has now.
- **Public classes.** `ValuesGrid`, `ResultsGrid`, `Titles` and `Links` are package-private today.
- **No `module-info`** (as `web` already has none; engine and sql keep theirs, which are inert on
  a class path) and **no `vaadin-quarkus-extension` dependency**: it compiles against `vaadin-core`
  only, so one jar serves Quarkus, Spring or plain Flow. Compile against the lowest Vaadin the
  consumers run (Admin: 25.2.7); nothing in the views needs a 25.3 API.
- **Release versions, not snapshots.** Elicit's poms disable remote snapshot resolution, so the
  jars are published as `0.1.0`+ to a repository Admin can reach (GitHub Packages under
  `com.elicitsoftware`, or `~/.m2` while iterating). BinaryFilter also needs a license: Admin's
  build stamps PolyForm Noncommercial on copied source but cannot on a jar.
- **Application concerns stay in `web`**: the library directory, declared connections, the
  scratch directory and its sweep, the open dialog. Admin writes its own equivalents against its
  datasources and `FilterWriter` — about 150 lines, all about *where files live*, which is the
  host's business.
- A host that builds a native image needs `-H:+SharedArenaSupport` (the engine maps files with
  `Arena.ofShared`); Admin is a JVM image.

**CSS.** The component jar ships `META-INF/resources/binaryfilter/facet-browser.css` and attaches
it with `@StyleSheet("context://binaryfilter/facet-browser.css")` **on `FacetBrowser`**. Flow
loads a component-level `@StyleSheet` the first time the component is used, Quarkus serves
`META-INF/resources` from any jar, and — unlike `@CssImport` — it needs no frontend bundle
rebuild and no `Vaadin-Package-Version` manifest, so Admin's `-Pproduction` build is untouched.
(`@CssImport` would be the choice only if the styles had to reach a component's shadow DOM; they
do not — grid state is exposed through parts.)

What the stylesheet may contain, so it renders under Aura and Lumo alike:

1. **Only scoped selectors**: the `bf-*` classes set at construction, the `has-selections`
   attribute, and `::part(selected)` / `::part(dimmed)` / `::part(count)` from the part-name
   generators. None of the five global rules `web-viewer.css` carries today — the `:root` token
   mapping, `html, body`, bare `vaadin-grid`, bare `vaadin-button`,
   `vaadin-app-layout::part(navbar)` — which would restyle every grid and button in the host.
2. **No `--lumo-*` tokens.** Admin renders Aura, and its Lumo tokens do not resolve. Use the base
   `--vaadin-*` properties with literal fallbacks (`var(--vaadin-padding-s, 0.5rem)`), which both
   themes define.
3. **Brand as optional hooks.** The component declares its own `--bf-*` properties with fallbacks —
   `--bf-selected-background: var(--brand-primary-10pct, rgba(0, 0, 0, 0.06))` — and the host sets
   them. Admin does so in one file, `components/facet-browser.css`, scoped under its view class,
   where `--brand-*` already arrive from the mounted brand; the web viewer keeps `web-viewer.css`
   for its own chrome (header, dialog, the vendored brand mapping) and does not repeat the
   component rules.
4. **Never `Lumo.STYLESHEET`** in the component; the application shell decides the theme.

BinaryFilter's `WebViewerStyleTest` asserts today's single-stylesheet structure and would split
into a component-stylesheet test (scoping, no `--lumo-*`, no global selectors) and an
application-stylesheet test.

**How Elicit's brand reaches the component.** A mounted brand (`brand.file.system.path`,
`/opt/brand` in compose) arrives on the page as CSS custom properties — `--brand-primary`,
`--brand-primary-10pct`, `--brand-header-background`, `--brand-font-family` and the rest of
`brand-colors.css` / `brand-typography.css` — served by Admin's `BrandResourceHandler` and
attached by `AppConfig`. Custom properties inherit down the DOM, into a component's light DOM and
through `::part()` into Vaadin's shadow parts, so the component's hooks resolve to the site's
brand with no per-brand code:

```css
.bf-viewer {
  --bf-selected-background: var(--brand-primary-10pct, rgba(0, 0, 0, 0.06));
  --bf-selected-color:      var(--brand-primary, inherit);
  --bf-count-color:         var(--brand-text-secondary, #6b7280);
  --bf-font-family:         var(--brand-font-family, inherit);
}
```

Swapping the brand directory re-brands the browser with the rest of Admin; the `var(…, fallback)`
form is what lets a partial brand (`test-partial-brand`) degrade cleanly. Two layers are covered:

1. **The component's own visuals** — selected and dimmed values, counts, the selection
   superscript, the find field, result links — come from the `--bf-*` hooks, a small set the
   component jar owns.
2. **The standard Vaadin parts inside it** — tab bar, grid headers, row hover, focus rings — are
   styled by the *host's* theme. Admin already maps `--brand-*` → `--vaadin-*` in its `styles.css`,
   so those parts brand themselves as every other Admin grid does. The component never performs
   that mapping, which is why it carries no `:root` block and no bare `vaadin-grid` rule.

The standalone web viewer reaches the same result through its vendored copy of `elicit-brand`, so
one component stylesheet serves both hosts. Choose the hooks' sources with the brand's semantics
in mind: the UM brand's `--brand-hover` / `--brand-active` are solid colors and must not sit under
dark text, so `--brand-primary-10pct` is the background token to use.

## 7. Reproducing the proof of concept

Files under `docs/research/faceted_exploration/`:

| File | Purpose |
|---|---|
| `synth-fhhs.sql` | Generates N finalized FHHS respondents (`-v N=3000`) directly into `survey.respondents`, `survey.subjects` and `survey.answers` — tagged answers only, question ids resolved at run time, layout asserted, rows marked `access_code LIKE 'SYN%'` / `xid LIKE 'SYN-%'`. Not re-openable in the Survey UI. |
| `synth-cleanup.sql` | Removes everything the generator created (fact rows first — no delete trigger) and reverts the dimension-name suffixes the run needed on any other survey sharing those step and section names (`survey_id <> 1`). |
| `relatives.sql`, `diagnoses.sql`, `respondents.sql` | The three recordsets of section 3; tab columns first (10, 7, 6). |
| `Check.java` | Opens the three `.bfilter` files through the engine API and prints the cascade counts. |
| `family-history-survey.elicit`, `family-history-survey-rework.md` | The FHHS definition with its reporting layer reworked under section 4, and the change log. Apply it to a scratch site instead of `FHHS/family-history-survey.elicit` to see the relabeled facets; it renames `other_age_key` and relabels history. |

```sh
# 1. data (dev stack up; db on localhost:5452, survey/admin)
docker exec -i elicit-db-1 psql -U survey -d survey -v N=3000 -f - < synth-fhhs.sql
# 2. star schema (synchronous; fails with dim_step_un if another survey shares a step name)
curl -s -X POST http://localhost:8080/api/etl/build
# 3. filter files — the jlinked bfilter carries no PostgreSQL driver by design, so run the producer jars
M=~/.m2/repository; export PGPASSWORD=admin
CP=$M/com/elicitsoftware/binaryfilter-producer/0.1.0-SNAPSHOT/binaryfilter-producer-0.1.0-SNAPSHOT.jar:\
$M/com/elicitsoftware/binaryfilter-engine/0.1.0-SNAPSHOT/binaryfilter-engine-0.1.0-SNAPSHOT.jar:\
$M/com/elicitsoftware/binaryfilter-sql/0.1.0-SNAPSHOT/binaryfilter-sql-0.1.0-SNAPSHOT.jar:\
$M/info/picocli/picocli/4.7.7/picocli-4.7.7.jar:$M/org/postgresql/postgresql/42.7.7/postgresql-42.7.7.jar
for spec in "relatives 10" "diagnoses 7" "respondents 6"; do set -- $spec
  java -cp "$CP" com.elicitsoftware.binaryfilter.producer.Main \
       --url jdbc:postgresql://localhost:5452/survey -u survey --password-env PGPASSWORD \
       --query-file $1.sql -t $2 -o $1.bfilter -f -v
done
# 4. cascade check; then browse with the desktop viewer (File > Open) or the web viewer
java --class-path $M/com/elicitsoftware/binaryfilter-engine/0.1.0-SNAPSHOT/binaryfilter-engine-0.1.0-SNAPSHOT.jar Check.java .
```

Add `--key Respondent` to the diagnoses conversion to try ADR 0014's And-within-a-tab.

## 8. Sequenced steps

None scheduled; this is dependency order.

1. **Survey — per-survey reporting schema** (5.1): naming, remove the `survey_id = 1` guards,
   `dimensions` per survey, ETL-fed `fact_respondents`, `POST /api/etl/build?survey=<key>`, both
   migration tracks, tests. Worth doing on its own.
2. **FHHS, Author, Admin — follow the namespace** (5.1 consumers).
3. **Author — reporting model extensions** E1–E4 (+ E6), preferably in the declared-role form
   of section 5.5, on the Reporting page and in the
   `.elicit` format; export-time refusals of 5.3 item 6; the guidelines into the manual and the AI
   spec (5.4).
4. **Survey ETL** — E1 typing and banding, label case, regenerate-on-change, column reclamation,
   identifier length guard.
5. **Recordset generation** from E1–E5 — a preview in Author, a build wherever the files are
   made.
6. **Where the browser lives** (6), on the evidence of 1–5.

## 9. Affected files (by step)

| Step | Files |
|---|---|
| 1 | `Survey/src/main/java/com/elicitsoftware/etl/{Sql,ETLService,ETLRespondentService,ETLBuildResource}.java`; `Survey/src/main/resources/db/migration/V002__Create_Reporting_Schema.sql` and its `migration-v3` counterpart; `Survey/src/test/java/com/elicitsoftware/etl/*`, `scd/DimStepSectionRekeySpecTest.java`, `flyway/ManualSchemaMigrator*Test.java`; `Survey/docs/use_cases/UC-008-rebuild-reporting-schema.md`, `Survey/docs/entity_model.md` |
| 2 | `FHHS/src/main/java/com/elicitsoftware/model/CancerHistoryRepository.java`, `FHHS/src/main/resources/db/migration/V0.0.6__Add_Performance_Indexes.sql`; the reporting-name and impact code of the authoring tool (separate repository); `Admin/src/main/java/com/elicitsoftware/service/ReportingSchemaRebuildClient.java`, a successor to `Admin/src/main/resources/db/migration/V0.0.2__ADMIN_GRANTS.sql` |
| 3 | The authoring tool's reporting model, tag dialogs and `.elicit` exporter/importer, plus its manual, AI-authoring spec, requirements and use cases (separate repository); Admin's `SurveyDefinition{Export,Import,Update,Apply}Service` |
| 4–5 | Survey ETL value handling and view generation; the authoring tool's Reporting page preview (separate repository) |
| docs | `CLAUDE.md`, `DeploymentScript.md`, `docs/manual/elicit-installation-manual.tex` (database chapter), `Admin/docs/vision.md` |
