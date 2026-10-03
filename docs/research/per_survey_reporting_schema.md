# One Reporting Schema per Survey: Removing the Survey-1 Restriction

> **Status (2026-10-02):** **Step 1 (Survey) is implemented** on
> `Survey:feature/per-survey-reporting-schema` (cea623b, a51d9ad): UC-008 rewritten, UC-010
> (rename) and UC-011 (drop) new, V021 on both tracks, every `Sql.java` statement a template over
> the survey's schema, `fact_respondents` a view, the two per-item columns of 3.6, 547 Survey
> tests green. Steps 2-5 (FHHS a9c6850, Admin f19652a, Author d4fda83, umbrella 606c0d4) were
> done the same day on branches of the same name, each module's suite green. Step 6, the same
> day: images rebuilt from the branches; **greenfield** (`Author/resetDatabase.sh V3`, `e2e-tests`
> 4/4) gave `report_family_history_survey` (22 tables, 15 `dim_step`) and the e2e survey its own
> schema with no `dim_step_un` warning; **brownfield** (`V2`, `e2e-tests` 4/4) ran V021 on the V2
> history (FHHS's history was already past V0.0.6, so Q-6's risk did not arise), dropped the
> triggers and the old star, regenerated 131 fact rows for the 3 pre-upgrade finished respondents
> with step and section keys resolving to the right names and `-1` in both per-item columns, and
> FHHS went UP. The two multi-site suites were not run. Nothing is pushed or merged yet. The
> questions of section 9 that the Survey code decided are marked answered there.
>
> **As found on 2026-10-01:** the reporting star schema (the
> Kimball schema Survey's ETL builds from a survey's reporting tags) works for **exactly one
> survey: the one whose `survey.surveys.id` is 1**. Every other survey's finished respondents
> get dimension values but no facts. Their step and section names collide with survey 1's in
> site-wide unique columns, and their tag columns land in the same table as survey 1's. This
> blocks the default reports (`default_reports.md`, G-3), the faceted browser
> (`faceted_exploration.md`) and any second survey on a site.
>
> **Four decisions were made while writing this:**
> - **Every survey gets its own reporting schema.** This confirms `faceted_exploration.md` §5.1.
> - **The schema name is a readable slug derived from the survey name and stored on
>   `survey.surveys`, and a site can change it.**
> - **The existing `surveyreport` objects on upgraded databases are regenerated from
>   `survey.answers`, not migrated.**
> - **The work starts after `i18n` is merged into `V3`**, on branches cut from the merged line.
>
> **Added 2026-10-02, a fifth decision:** a section repeated once per selected option of a list
> reports **which question and which option** each of its rows belongs to, in two new columns of
> `fact_sections` (section 3.6). It is built as part of this work, not ahead of it, because it
> lives in the tables and the ETL this document rewrites.
>
> **Added 2026-10-02, a sixth decision:** deleting a survey in Author also deletes that survey's
> reporting schema in Author's database, the one the preview Survey (`author-survey`) builds
> (section 3.7). A deleted survey leaves no schema behind.
>
> Sections 9 and 10 list what is still open.

## Overview

`surveyreport` was designed for one survey per site, the Family History Survey. Survey owns it:
- V002 creates the fixed tables.
- The ETL adds a `dim_<tag>` table and a `<tag>_key` column on `fact_sections` for every tag it
  finds.
- Two triggers on `survey.respondents` keep `fact_respondents` current.

Supporting several surveys was never designed in. The single-survey assumption survives in five
forms:
1. **Hard-coded guards.** `survey_id = 1` appears in both triggers and in two ETL queries.
2. **Unscoped queries.** Most ETL statements read every survey's tags and steps.
3. **Site-wide uniqueness.** `dim_step` and `dim_section` are unique on `value` across the site.
4. **Shared structure.** One `fact_sections` and one `fact_sections_view` serve the whole site.
5. **Display orders as keys** (found while implementing, 2026-10-02). `INSERT_MISSING_FACT_SECTION_SQL`
   wrote `answers.step` and `answers.section` — the step's display order and the section's display
   order *within the step* — straight into `fact_sections.step_key` and `section_key`, whose foreign
   keys point at `dim_step.id` and `dim_section.id`. `step_key` was right only while a step's
   surrogate id equaled its display order (true for a first survey whose steps were inserted in
   order, never for a second survey, whose ids start where the first one's end). `section_key` was
   right only for the first section of a step: every other section's row pointed at whatever
   section held that low id, so `fact_sections_view.section` named the wrong section for them.
   FHHS never noticed because it reads `step` and the tag columns, not `section`. Survey UC-008
   BR-011 now resolves both to the dimension ids as of the respondent's anchor.

Removing the guards alone would turn (2)–(4) from latent bugs into live ones. The fix is
structural: each survey's star lives in its own schema, and every statement names that schema.

Terminology:
- A **survey schema** is the per-survey reporting schema this document proposes.
- The **common schema** is `surveyreport` after the change, holding only what every survey
  shares.
- **Regenerate** means rebuilding a survey schema from `survey.answers` through the back-fill.
- **Slug** means the schema name stored on the survey.

## 1. What is wrong today

### 1.1 The guards

| Where | Guard | Effect for a survey other than 1 |
|---|---|---|
| `Survey/.../db/migration/V002__Create_Reporting_Schema.sql:127` (and `migration-v3` `:119`) | `insert_fact_respondent()`: `IF (TG_OP = 'INSERT' AND new.survey_id = 1)` | no `fact_respondents` row |
| same file `:157` (`migration-v3` `:149`) | `update_fact_respondent()`: same guard | never updated |
| `Survey/.../etl/Sql.java:277` | `NEW_RESPONDENTS_SQL`: `r.survey_id = 1` | unused; the constant is referenced only by javadoc |
| `Sql.java:314` | `INSERT_MISSING_FACT_SECTION_SQL`: `a.survey_id = 1` | **no `fact_sections` rows**, so nothing in `fact_sections_view` |

Survey 1 is whichever survey holds id 1 on that database, not necessarily the Family History
Survey. On a site that imported another survey first, FHHS's reports would come back empty.

### 1.2 Unscoped statements

None of these filter by survey (all in `Sql.java`):

| Statement | Reads | Consequence |
|---|---|---|
| `UPDATE_STEPS_DIMENSION_TABLE_SQL` :72, `UPDATE_SECTIONS_…` :81 | every survey's current steps and sections | a second survey with a step name already used fails `dim_step_un` and the rebuild returns FAILED. This is documented at `ETLService.java:152-156`, and it is why Author's preview Survey runs with `elicit.etl.enabled=false` (`Author/docker-compose.yml:265-267`) |
| `FIND_NEW_DIMENSION_TABLES_SQL` :89 | every survey's metadata | `dim_<tag>` tables are shared by name across surveys |
| `FIND_DIMENSIONS_TO_ADD_TO_FACT_SECTIONS_TABLE` :362, `FIND_FACT_SECTION_JOIN_COLUMNS` :408 | every survey's ontology | every survey's tag columns end up on the one `fact_sections` and in the one view. FHHS's 66 columns would appear on every survey's rows |
| `FIND_MISSING_FACT_SECTION_RESPONDENTS` :643 | every finished respondent | every rebuild re-selects every finished non-survey-1 respondent, inserts nothing for them because of the guard, and selects them again next time |
| `INSERT_ALL_RESPONDENTS_INTO_FACT_RESPONDENTS_SQL` :327 | every respondent | unused |

`FIND_DIMENSTION_VALUES_SQL` (:160) is respondent-scoped and joins `metadata.survey_id =
answers.survey_id`. That is why other surveys' values do reach the shared `dim_*` tables while
their facts do not.

### 1.3 Constraints in the `survey` schema

| Constraint | Where | Bearing |
|---|---|---|
| `dimensions_un UNIQUE (name)`; no `survey_id` column | `V001:745-751` | dimension names are site-wide. A survey schema does not need this changed (section 9, Q-4) |
| `ontology_un UNIQUE (name, tag)`, without `survey_id` | `V001:758-768` | the same (namespace, tag) pair cannot appear in two surveys. Author's namespace is the survey name, so collisions are unlikely but possible |

### 1.4 A date problem found on the way

`dim_date` holds 19700101 and 2020-01-01 through 2029-12-31 (`V004:16-3670`).
`fact_respondents.created_key` and the other date keys have foreign keys to it. The insert trigger
runs inside the `survey.respondents` insert, so **from 2030-01-01 registering a respondent in
survey 1 fails with a foreign-key violation**. The same applies to the update trigger on first
login and on finish. This document removes the triggers (section 3.4), which also removes the
problem. Until then it is a dated defect worth recording on its own.

## 2. Target design

```
surveyreport            (common)  dim_date, dim_status
report_family_history   (survey)  dim_step, dim_section, dim_question, dim_item, dim_<tag>…,
                                  fact_sections, fact_sections_view, fact_respondents (view),
                                  fact_respondents_view
report_household        (survey)  … the same set, for another survey
```

- **Identifiers inside a survey schema stay byte-identical** to today's (`dim_<tag>`,
  `<tag>_key`, `fact_sections_view`), and so does `ReportingNames` in Author. Only the schema
  qualifier changes, so FHHS's column list, the faceted-exploration SQL and analysts' queries change
  by one word.
- **Uniqueness becomes per survey for free.** `dim_step_un` and `dim_section_un` keep their
  definition but apply inside one survey.
- **The common schema keeps its name.** `surveyreport` already exists on every site, and the
  installation manual creates it with its grants (`docs/manual/elicit-installation-manual.tex:306-316`).
  Keeping the conformed dimensions there means operators create nothing new by hand. Survey
  schemas are created by the ETL as `elicit_owner`, which the manual already allows
  (`GRANT CONNECT, CREATE ON DATABASE survey TO dbowner`, :297).
- **`survey_id` stays on `fact_sections`.** It is redundant inside a survey schema, but analysts'
  `UNION`s across surveys stay possible.
- **Two fixed columns are new:** `fact_sections.question_key` and `item_key`, with `dim_question`
  and `dim_item` behind them (section 3.6). Every other identifier is unchanged, so a query that
  names its columns is unaffected.

## 3. Survey changes

### 3.1 The name: `survey.surveys.report_schema`

The name is a new nullable column, unique, assigned once by the ETL the first time it builds the
survey. The assignment rule:
- `report_` + the survey name, lower-cased.
- Characters outside `[a-z0-9]` become `_`, repeats collapse, and the ends are trimmed.
- The result is truncated to 63 bytes.
- If the name is taken, `_2`, `_3`… is appended.

**It is stored, not recomputed.** A later rename of the survey does not move its schema, and
external queries keep working.

The column is site-local data and is **not** carried in the `.elicit` file. Two sites applying the
same survey may choose different names, and an import or update never touches it.
`Sql.requireValidIdentifier` gains a schema form: `^[a-z_][a-z0-9_]{0,62}$`, not `surveyreport`,
`survey`, `public`, nor `pg_*`/`information_schema`. This also closes
`faceted_exploration.md` §5.3 item 4, the missing length guard, for schema names.

### 3.2 Changing the name

The user asked that sites be able to rename. The rename is an operation, not an edit of the
column:
- `elicit_owner` owns the schema.
- Survey is the module connected as `elicit_owner` for reporting.
- Admin connects as `surveyadmin_user`, which cannot `ALTER SCHEMA`.

Survey therefore exposes it, for example `POST /api/etl/schema/{surveyKey}/rename` with
`{"name": "report_fhh"}`. In one transaction under the rebuild lock it:
1. validates the name (3.1);
2. runs `ALTER SCHEMA <old> RENAME TO <new>`;
3. updates `surveys.report_schema`.

PostgreSQL resolves views, sequence defaults and foreign keys by OID, so everything inside the
schema keeps working. Grants and ownership move with it. Admin offers the action on the survey's
page, with the warning that external queries and BI connections that name the old schema stop
working.

Inside Elicit, **nothing may hard-code a survey schema's name**. Every module looks the name up
from `survey.surveys` at query time: FHHS by its `family.history.survey.key` (section 6), and
Survey's ETL by survey id.

### 3.3 The ETL

Every statement in `Sql.java` gains `<SCHEMA>` and `:surveyId`:

| Group | Change |
|---|---|
| Table and column discovery (`FIND_NEW_DIMENSION_TABLES_SQL`, `FIND_DIMENSIONS_TO_ADD_…`, `FIND_FACT_SECTION_JOIN_COLUMNS`) | `information_schema` filters on `table_schema = :schema`; `metadata`/`ontology` joins filter on `survey_id = :surveyId` |
| Step/section upserts | `WHERE s.survey_id = :surveyId`, target `<SCHEMA>.dim_step` |
| DDL templates (`CREATE_NEW_DIMENSION_TABLE_SQL`, `ADD_DIM_COLUMN_…`, views) | `<SCHEMA>.` instead of `surveyreport.`; the views join `surveyreport.dim_date` / `dim_status` |
| Fact inserts and back-fill | drop `a.survey_id = 1`; `FIND_MISSING_FACT_SECTION_RESPONDENTS` adds `r.survey_id = :surveyId` |
| Dead code (`NEW_RESPONDENTS_SQL`, `INSERT_ALL_RESPONDENTS_…`, `NEW_FIND_MISSING_…`, `FACT_SECTIONS_KEYS`, `ADD_DIM_COLUMN_TO_FACT_ANSWER_TABLE`, the commented methods in `ETLService`) | delete |

The three entry points change as follows:

| Entry point | Today | Proposed |
|---|---|---|
| Startup (`ETLService.init`) | builds once if `surveyreport.dim_section` is empty, then back-fills everyone | for each survey: create the schema if `report_schema` is null, run the idempotent build, back-fill that survey's finished respondents |
| `POST /api/etl/build` (`ETLBuildResource`) | rebuilds the whole site | takes `?survey=<survey_key>` and rebuilds that survey. Without it, all surveys, each in its own transaction, with per-survey results in the reply |
| Finish (`ETLRespondentService.populateFactSectionTable`) | per respondent, shared schema | per respondent, into the survey's schema. If the schema does not exist yet (its build failed), log and return. The next build's back-fill picks the respondent up |

A new step, *create survey schema*, runs `CREATE SCHEMA <name> AUTHORIZATION elicit_owner`, grants
`USAGE` and default `SELECT` privileges to `surveyreport_user`, and grants `USAGE` plus the
`fact_sections` `INSERT/SELECT/UPDATE` that `survey_user` holds today. It then creates `dim_step`,
`dim_section` and `fact_sections` from the V002 definitions, now as Java templates, with the
generic indexes from Survey V008 and FHHS V0.0.6 (section 6). `dim_question`, `dim_item` and the
two `fact_sections` columns that refer to them (3.6) are part of the same templates.

### 3.4 `fact_respondents` becomes a view

The decided design said "filled by the ETL". The ETL, however, runs only on finish and on a build,
so an ETL-filled table would show every in-progress respondent as Not Started until the next build.
**A view is simpler and always current:**

```sql
CREATE VIEW <SCHEMA>.fact_respondents AS
  SELECT r.id, r.survey_id, r.active, r.logins,
         COALESCE(to_char(r.created_dt,'YYYYMMDD')::int, 19700101)      AS created_key,
         COALESCE(to_char(r.first_access_dt,'YYYYMMDD')::int, 19700101) AS first_access_key,
         COALESCE(to_char(r.finalized_dt,'YYYYMMDD')::int, 19700101)    AS finalized_key,
         CASE WHEN r.first_access_dt IS NULL AND r.finalized_dt IS NULL THEN 0
              WHEN r.finalized_dt IS NULL THEN 1 ELSE 2 END             AS status,
         …duration…
  FROM survey.respondents r WHERE r.survey_id = <id>;
```

- The view runs with its owner's rights, so `surveyreport_user` needs no grant on `survey`.
- The triggers and their functions are dropped. This removes the cross-schema write from every
  respondent insert and update, and the 2030 failure (1.4).
- `fact_respondents_view` joins `dim_date` with a `LEFT JOIN`, so a date beyond the table gives a
  null label instead of a missing row. Extending `dim_date` itself is Q-5.
- The duration definition (created vs first access, `default_reports.md` G-9) is settled here, once.

### 3.5 Migrations: both tracks

Following the two-track rule, every migration goes in both `db/migration` and `db/migration-v3`.

**Greenfield (`db/migration`, unreleased and rewritable):**
- V002 shrinks to `dim_date` and `dim_status` in `surveyreport`, with no triggers.
- V008 drops its `fact_sections` indexes; the ETL creates them per schema.
- A new migration adds `surveys.report_schema`.

**Upgrade (`db/migration-v3`), one new migration:**
1. Drop the two triggers and their functions.
2. Drop `surveyreport.fact_sections_view`, `fact_respondents_view`, `fact_sections`,
   `fact_respondents`, `dim_step`, `dim_section` and every other `dim_*` except
   `dim_date`/`dim_status`, together with their sequences.
3. Add `surveys.report_schema`.

The next startup then **regenerates** every survey's schema from `survey.answers` (the user's
decision). The cost:
- Surrogate ids differ from before, so any external extract keyed on `fact_sections.id` or
  `dim_*.id` must be re-pulled.
- Answers soft-deleted before Finish were already purged, so nothing reported today is lost.

The two columns of 3.6 add nothing to either track: `fact_sections` and its dimensions are created
by the ETL's templates, not by a migration, and nothing in the `survey` schema changes for them.

The dropping migration and the regenerating startup are not one transaction. A startup that fails
after the drop leaves a site with no reporting tables until a successful build. This is acceptable
because reports are rebuilt from answers, but it must be stated in the upgrade procedure
(`DeploymentScript.md`).

### 3.6 Which option a repeated section belongs to

**The problem.** A Repeat rule that reads a `MULTI_SELECT` or `CHECKBOX_GROUP` builds one instance
of its target per option the respondent selected (Survey UC-002 A3b, on
`feature/multi-select-repeat`, not yet merged). The instance number is the option's **position in
its list** as of the respondent's snapshot anchor (UC-002 BR-012), which is what keeps a
respondent's answers with the option they are about when the selection changes. A position is not
an identity across revisions, though:

- Oklahoma is the second option, so its rows carry `section_instance = 2`.
- The list is reordered, or an option before it is removed, and the survey is applied again.
- Respondents who start afterwards get Oklahoma at another position, and `section_instance = 2`
  is now a different game.

Nothing stored is wrong: earlier respondents keep their rows, and a respondent already answering
keeps the list as it was. But `fact_sections` has no other column that says which option a row is
about, so "concession quality by game" can only be grouped by `section_instance`, and after the
reorder that silently mixes two games.

**The design** (the user's, 2026-10-02): the fact row names the question and the option, by the
keys that never change.

| Column | Refers to | Filled |
|---|---|---|
| `fact_sections.question_key` | `dim_question.id` | the question the Repeat rule reads |
| `fact_sections.item_key` | `dim_item.id` | the option this instance was built from |

Both follow the convention of every `<tag>_key` column: an integer, `NOT NULL DEFAULT -1`, a
foreign key to a dimension that carries the `(-1, NULL)` "no value" member.

| Dimension | Natural key | Attributes |
|---|---|---|
| `dim_question` | `survey.questions.question_key` (UUID) | `value` = the question's short text |
| `dim_item` | `survey.select_items.select_item_key` (UUID) | `value` = coded value, plus display text, list name and display order |

The natural keys are the portable element keys: copied forward on every version of the row and
identical at every site that installed the definition. A dimension row is upserted by that key, as
`dim_step` is by its durable id, so rewording an option, reordering the list or even changing an
option's coded value updates one row and moves no fact. The question is there for the *role*: one
list can serve two questions, and the question says which of them a row answers, which is also what
gives the column a name a facet can use. `fact_sections_view` exposes both as `question` and
`item`, joined like any tag.

**Only where it is needed.**

- Filled for a section instance built **from a selected option**. Every other row keeps `-1`.
- A **count-driven** repeat needs nothing: its instances are 1 to N with no list behind them, so
  there is no order to alter and the instance number is the identity.
- A step shown once per repeated **free-text** answer (the Family History Survey's siblings and
  children) is not touched. Its driving answer is a name, and a name must not reach a reporting
  table (`faceted_exploration.md` G7).
- A repeated **question** has no fact row of its own: several answers share one section instance.
  It stays unreportable per option here, as it is today.
- When a step can be repeated (`repeating_steps.md`), a step repeated per option fills the same two
  columns; the instance it reads is then the step instance.

**How the ETL fills them.** From the definition, as of the respondent's anchor, with no new column
in the `survey` schema. For a fact row with `section_instance > 0`:

1. Find the Repeat rule in effect at `respondents.first_access_dt` whose downstream placement is
   the row's step and section and whose upstream question is a `MULTI_SELECT` or `CHECKBOX_GROUP`.
   No such rule: both columns stay `-1`.
2. `question_key` is that upstream question.
3. `item_key` is the option at position `section_instance` of the question's list, as of the same
   anchor, ordered by `display_order` and then `select_item_id`: exactly the order
   `QuestionManager.repeatItems` builds the instances in.

Deriving, rather than storing the option on the answer when the instance is built, was chosen
because of what it leaves alone:

- No migration of `survey.answers` in either track, and nothing to back-fill.
- Admin's answer transfer between sites keeps its record layout.
- **Regeneration reproduces it.** The upgrade drops and rebuilds every survey schema from
  `survey.answers` (3.5); a value derived from the definition and the anchor comes back the same.

The cost is that the position rule exists twice, in Java and in SQL. Section 8 has the test that
holds the two together.

**Brownfield.** A V2 database cannot hold a per-option instance: before UC-002 A3b a Repeat reading
a multi-select failed on the page. Every regenerated row therefore carries `-1` in both columns, and
the conversion has nothing to convert. It still gets the brownfield run of section 8.

**No warning in Author.** A warning at export when a list that drives a Repeat is reordered was
considered as a stopgap for sites on a build without these columns, and dropped: V3 has never been
released, so there is no such site.

### 3.7 Deleting a survey drops its schema

**The requirement** (the user's, 2026-10-02): when a survey is deleted in Author (Author UC-047),
its reporting schema is deleted with it. The database is Author's own, `author`, and the Survey
instance that builds schemas there is the preview, `author-survey`
(`Author/docker-compose.yml:256`).

**Why it does not happen by itself.** UC-047 deletes rows of the `survey` schema, in one
transaction, as `survey_user` (`Author/.../survey/SurveyService.java:234-265`). A survey schema is
a separate object owned by `elicit_owner`:

- No foreign key ties it to `survey.surveys`, so deleting the row leaves it standing.
- The row held the only record of its name (`report_schema`), so afterwards nothing in Elicit can
  find it.
- Its facts describe preview respondents UC-047 has just deleted (BR-004), and its
  `fact_respondents` view (3.4) selects a `survey_id` that no longer exists.
- The backup, imported again and built, would be assigned `report_<slug>_2` beside the orphan,
  because the name is taken (3.1).

`survey_user` cannot drop it. As with the rename (3.2), Survey is the module that can.

**The design.** Survey exposes `DELETE /api/etl/schema/{surveyKey}`. Under the rebuild lock, in
one transaction, it:

1. reads the survey's `report_schema`; if it is null, answers 200 with nothing dropped;
2. runs `DROP SCHEMA <name> CASCADE`;
3. sets `surveys.report_schema` to null.

The call is idempotent, and it works whether or not `elicit.etl.enabled` is set: a schema built
while the ETL was on still has to go after it is turned off.

Author's delete then runs in this order:

| Step | Who | What | If it fails |
|---|---|---|---|
| 1 | Author | reads `report_schema`; null means there is nothing to drop, continue at step 3 | |
| 2 | Author → preview Survey | `DELETE /api/etl/schema/{surveyKey}` | the delete is refused and the survey is unchanged, which is UC-047's failure postcondition |
| 3 | Author | the existing delete transaction | the survey remains, without a schema; the next build creates it again from the preview answers |

**The schema goes first** so that neither failure leaves an orphan. In the other order, a drop
that failed after the row was gone would leave a schema nothing names.

Author has no address for the preview Survey today: the two share the `author` database and
nothing else. Step 2 needs a new Author property for it (in compose, `http://author-survey:8080`)
and a client with timeouts.

**Considered and not proposed:** a `SECURITY DEFINER` function owned by `elicit_owner` that Author
calls inside its own transaction. It would make the drop and the delete atomic and need no HTTP
call, but it would put a schema-dropping function within reach of `survey_user` on every site,
the role the respondent-facing application connects as.

**Today this is a no-op.** The preview runs with `elicit.etl.enabled=false`
(`Author/docker-compose.yml:267`), so no survey in `author` has a schema and step 1 always
continues at step 3. The preview's ETL stays off for now (Q-11, the user's, 2026-10-02) and is
expected to be turned on when the reporting work starts, because that work needs the preview to
model what a site builds. The drop is built with the rest of this change so that it is already in
place on that day.

**Sites.** Admin has no survey deletion (no use case and no code, searched 2026-10-02), so
nothing at a site calls the endpoint. It is still present on every Survey, because the preview is
the same image, and Survey's REST endpoints are unauthenticated
(`ETLBuildResource.java:39-45`). The build endpoint's defense, that it can do nothing a restart
would not, does not hold for a drop: the next build regenerates the schema, but with new
surrogate ids (3.5). Q-12.

## 4. Admin changes

| What | Where | Change |
|---|---|---|
| Rebuild after apply | `service/ReportingSchemaRebuildClient.java:51,134` | pass `?survey=<key>` of the survey just applied |
| Grant | `db/migration/V0.0.2__ADMIN_GRANTS.sql:30` gives `surveyadmin_user` `INSERT, SELECT, UPDATE` on `surveyreport.fact_respondents` | no Admin code uses it. A new migration revokes it before Survey's upgrade drops the table. Ordering is Q-6 |
| Rename | new | an action on the survey's page that calls Survey's rename (3.2), with the warning. New UC, FR, translation keys with `es-419`/`ar`, and the translation request |
| Test bootstrap | `src/test/resources/db/test/V0.0.0.1__TEST_BOOTSTRAP.sql:674-681` | drop the `fact_respondents` stub |

## 5. Author changes

- `ReportingView.java:219,226` shows authors `surveyreport.<dim>` and `fact_sections.<col>`, and
  three translation keys (`translations.properties:426,475,502`) say "surveyreport schema". After
  the change the schema is site-chosen and unknown to Author. Show the unqualified table names and
  say "the survey's reporting schema". This combines naturally with the rename to *analysis tags*
  proposed in `default_reports.md` §2.3.
- `ReportingService.java:357-370` warns about cross-survey `fact_sections` column collisions ("one
  table per site"). The warning becomes obsolete and is removed.
- The `schema-mirror` test copies of Survey's V002/V004/V008 follow the greenfield rewrite.
  `SchemaMirrorFreshnessTest` enforces this.
- **The two fixed columns (3.6).** A reporting tag named `Question` or `Item` would produce the
  column `question_key` or `item_key` and collide with them; nothing reserves a tag name today
  (`ReportingNames`). Either the names are reserved, with a validation finding, or the columns take
  another name (Q-8). The dimensional-impact preview (UC-024) lists the two columns and their
  dimensions for a survey that has a Repeat reading a multi-select.
- **Wording that goes stale.** The author's manual cautions that "the order of the list is part of
  the survey", and UC-019 BR-007 says the same. With 3.6 a reorder changes the order of a new
  respondent's pages and nothing in reporting; both are reworded to say that.
- `db-init/V0.0.4__CREATE_AUTHOR_DATABASE.sql:22-33` keeps creating `surveyreport` (still the
  common schema).
- **Delete Survey (UC-047, FR-065) drops the survey's schema first (3.7).** `SurveyService.delete`
  reads `report_schema` and, when it is set, calls the preview Survey's drop before its own
  transaction; a failed drop refuses the delete. New: the property naming the preview Survey, the
  client, a business rule in UC-047 beside BR-004, and the message for a refused delete (with
  `es-419`/`ar` and the translation request). `SurveyDeleteTest` covers both orders of failure.
- The preview Survey's `elicit.etl.enabled=false` loses its reason: the collision is gone. What
  is left is that a drafts database grows one schema per previewed draft, and 3.7 bounds that by
  removing a schema with its survey. The ETL stays off for now and is expected to be turned on
  when the reporting work starts (Q-11); the comment at `ETLService.java:68-73` and in the
  compose file is updated to say so.

## 6. FHHS changes

FHHS is a consumer of the schema, not the target of this work, but it is the one consumer that
must follow in the same release.

| What | Where | Change |
|---|---|---|
| The report query | `model/CancerHistoryRepository.java:191` reads `surveyreport.fact_sections_view` | resolve the schema from `survey.surveys.report_schema` by `family.history.survey.key` (`application.properties:190`), already used by the readiness check (`FamilyHistorySurveyCheck.java:42-56`), and qualify the query with it. Cache per request, not per process, so a rename takes effect at once. Columns are still selected by name and mapped by position (:216-284); the column list does not change |
| Readiness | FHHS UC-005 | not ready also when `report_schema` is null (survey imported but never built); the 503 says so |
| Indexes | `db/migration/V0.0.6__Add_Performance_Indexes.sql`, both tracks | on greenfield `surveyreport.fact_sections` no longer exists, so this migration **would fail**. The greenfield copy is rewritten to the indexes on `survey` tables only (FHHS `db/migration` is unreleased). The upgrade track needs an FHHS migration ordered before Survey's drop, or `IF EXISTS` guards. The fact-table indexes move into Survey's per-schema template (3.3) |
| The old union view | V0.0.3 / V0.0.7 (`migration-v3`) | unchanged; they create and then drop it before Survey's drop runs |
| Tests | `db/test/V0.0.0.1__TEST_BOOTSTRAP.sql:55,573-623,710` (and `test-legacy`), `CancerHistoryRepositoryTest.java:31,96`, `ManualSchemaMigratorUpgradeTest.java:124` | the bootstrap creates a survey schema and sets `report_schema`; the test asserts the resolved qualifier |
| The two new columns (3.6) | `fact_sections_view` gains `question` and `item` | none: FHHS selects its columns by name, and the Family History Survey has no Repeat reading a multi-select, so every one of its rows carries "no value" in both |

FHHS is on `V3` locally while Survey and Admin are on `i18n`. The merge the user plans first puts
all four repositories on one line before any of this starts.

## 7. Umbrella documents

| Document | Change |
|---|---|
| `CLAUDE.md` :138-146 | the check `select count(*) from surveyreport.dim_step` becomes a lookup of `report_schema` and a count in that schema; "surveyreport grows from its six skeleton tables" is rewritten |
| `docs/manual/elicit-installation-manual.tex` :129, :253, :279-316, :549, :572, :1168 | `surveyreport` is now the common schema; survey schemas are created by the application; `dbowner` needs `CREATE` on the database (already granted, now required); the narrowed-owner caution (:285-290) adds "create schemas"; the configuration reference picks up the rename endpoint if it gets a property, and the drop endpoint's switch if Q-12 gives it one. `check-properties.sh` keeps the manual honest |
| `docs/use_cases/UC-005-prepare-the-database-cluster.md:61` | "empty `survey` and `surveyreport` schemas" stays true; add that survey schemas appear after the first build |
| `DeploymentScript.md` | the upgrade step: what the `migration-v3` migration drops, that the next startup regenerates, and that extracts must be re-pulled |
| `docs/research/faceted_exploration.md` §5.1 | the two open items (naming key, migrate vs regenerate) are answered here |
| `docs/research/default_reports.md` G-3 | points here |
| `docs/research/faceted_exploration.md` G6, E6 | a section repeated per selected option is one fact row per option with `item` as its value: the multi-select split E6 asks for, for the questions an author chooses to repeat on. G6 gains it as the third way to model a multi-choice |
| `samples/README.md`, `samples/generate-census-household-survey.py` | the census survey's per-language repeat is a repeated *question*, which 3.6 does not cover; say so where the sample explains why it is untagged |

## 8. Verification

| Run | Proves |
|---|---|
| Survey unit/`@QuarkusTest`: two surveys in the fixture, with overlapping step names and the same tag name | each gets its own schema; no `dim_step_un` failure; `fact_sections` columns are disjoint; back-fill is per survey and terminates |
| Rename test | `ALTER SCHEMA` plus the column update; views still answer; an invalid or taken name is refused and nothing changes |
| `fact_respondents` view | status moves Not Started → In Progress → Finished without any build; a `created_dt` in 2031 still appears |
| Greenfield (`resetDatabase.sh V3`), import FHHS then a second survey, finish one respondent in each | two schemas; FHHS reports unchanged; the default reports' prerequisite holds |
| Brownfield (`resetDatabase.sh V2`) | the upgrade drop runs, startup regenerates, FHHS reports match the pre-upgrade output for the same respondents (required: FHHS changes always get a brownfield run). Every regenerated row carries `-1` in `question_key` and `item_key` (3.6) |
| Import order | the second survey imported first: FHHS still reports, because nothing depends on id 1 |
| Per-option repeat, `@QuarkusTest` (3.6) | a respondent selects the second and fourth options and finishes: two fact rows, `section_instance` 2 and 4, each with the question and **the option whose text titled that instance at runtime**. This is the test that holds the SQL position rule to the Java one |
| Reorder across a revision (3.6) | respondent A finishes; the list is reordered and the definition applied again; respondent B selects the same option and finishes. The two rows have different `section_instance` and **the same `item_key`**; a count-driven repeat and a step shown per free-text answer in the same survey carry `-1` in both columns |
| Regenerate (3.6) | drop the survey schema and rebuild from `survey.answers`: both respondents' rows come back with the same question and option |
| Drop, Survey `@QuarkusTest` (3.7) | the schema and everything in it are gone and `report_schema` is null; a second call answers 200 and changes nothing; another survey's schema is untouched; the next build creates the schema again |
| Delete in Author (3.7) | a survey with a schema: after UC-047 neither the row nor the schema exists, and its backup, imported and built, is assigned the same schema name, not `_2`. With the preview Survey unreachable the delete is refused and the survey, its preview answers and its schema are unchanged. A survey with a null `report_schema` deletes without any call |
| Author e2e and multisite | the preview instance still starts; `DesignerPage.java:38`'s unique-name workaround can stay but is no longer needed |

## 9. Research questions

1. **Q-1 Multi-survey fixture.** Survey's test data has one survey (`V9005*`, "Library Card
   Registration"). Build a second, small survey fixture with deliberately overlapping names before
   touching `Sql.java`, so the failure is reproduced first.
2. **Q-2 Transaction boundaries.** Answered by the Survey code (2026-10-02): each step of a
   survey's build commits on its own, as before, and the startup and the all-surveys build log a
   failed survey and continue with the next (UC-008 A2, A7). A half-built schema can therefore
   exist, but every step only creates what is missing, so the next build completes it; the
   schema's creation itself (schema, grants, fixed tables) is one `IF NOT EXISTS` script in one
   transaction. One transaction per survey was not done: the step methods are the
   `@Transactional` units the rebuild lock serializes, and a failure message per step is what an
   operator acts on.
3. **Q-3 Survey deletion.** Answered. Author can delete a survey (UC-047) and its schema is
   dropped with it (3.7, the sixth decision). Admin cannot: it has no use case and no code that
   deletes a survey, so a site's schemas are never removed. If Admin gains one, it calls the same
   endpoint.
4. **Q-4 Dimension scope.** `dimensions_un` and `ontology_un` are site-wide. With per-survey
   schemas they no longer *need* to be, but making `dimensions` per survey touches `survey` DDL,
   Author and the `.elicit` format. Confirm it can stay out of this change and remain
   `faceted_exploration.md` §5.5 work.
5. **Q-5 `dim_date`.** Answered for now (2026-10-02): `dim_date` is not extended; the
   `fact_respondents_view` of every survey joins it with `LEFT JOIN` (UC-008 BR-010), so a date
   outside 2020-2029 gives a null label and never hides a respondent, and the triggers that failed
   on it are gone. Extending the table to 2100 remains open as a small follow-up in the common
   schema.
6. **Q-6 Flyway ordering across modules.** Survey, Admin and FHHS each run their own history
   table against the same database at startup. Admin's revoke and FHHS's index migration must not
   run after Survey's drop and fail on a missing table. Verify with `IF EXISTS` everywhere, or
   make Survey's upgrade migration tolerate dependents.
7. **Q-7 Rename and concurrency.** Answered (2026-10-02): the rename, the drop and every build
   take the same in-process rebuild lock (UC-008 BR-005, UC-010 step 2, UC-011 BR-002). The
   finalize-time load does not take it; it reads `report_schema` at the start of its own
   transaction, so a load that started before a rename finishes against the old name
   (PostgreSQL resolves the rename by object id, so the writes land) and the next one sees the
   new name (UC-010 Notes).
8. **Q-8 The two column names.** Answered (2026-10-02): the names stay `question_key` and
   `item_key`, and the tag names are reserved. Survey's build refuses a survey whose ontology has a
   tag that would produce `step_key`, `section_key`, `question_key` or `item_key` (UC-008 BR-012,
   `Sql.FIND_RESERVED_TAGS_SQL`), failing with the tag named rather than silently reusing a fixed
   column. Author should report the same four names as a validation finding before export (step
   4).
9. **Q-9 Two Repeat rules on one section.** Answered in the ETL (2026-10-02): the fill query
   takes one rule per fact row (`SELECT DISTINCT ON (fact_id)`, the first by fact id), so a second
   Repeat reading another multi-select onto the same placement never produces two values and
   never fails the build. Which of the two wins is not defined; whether Author forbids the second
   rule is still Author's to confirm (step 4).
10. **Q-10 `dim_item.value`.** Answered (2026-10-02): `dim_item.value` is
    `lower(trim(coded_value))`, as every tag dimension's value is, so `fact_sections_view.item`
    reads like the tag columns beside it; `display_text` (base language only, C-024),
    `list_name` and `display_order` sit beside it as attributes for a report to show.
11. **Q-11 The preview's ETL.** Answered (the user's, 2026-10-02): `elicit.etl.enabled` stays off
    for `author-survey` in this change. It is expected to be turned on when the reporting work
    starts (the default reports, the faceted browser), because that work has to model the real
    system: the star a site builds from the same definition. Until then 3.7 is a guard that never
    fires. Once it is on, every previewed draft gets a schema until it is deleted (3.7), and a
    survey renamed in Author keeps its first slug (3.1); both are to be looked at again then.
12. **Q-12 Who may call the drop.** Answered (2026-10-02): `elicit.etl.drop.enabled`, a Survey
    property, `false` by default; `DELETE /api/etl/schema/<key>` answers 403 while it is off
    (UC-011 BR-001, A2). The Author stack's compose sets it `true` on `author-survey`. The
    installation manual's configuration reference lists it with the advice to leave it off at a
    site.
13. **Q-13 A build between the drop and the delete.** After step 2 of 3.7 the survey still exists
    with a null `report_schema`. A preview respondent finishing in that window is harmless (3.3:
    no schema, log and return), but a startup or a `POST /api/etl/build` would create the schema
    again and the delete would then orphan it. Confirm how narrow the window is, or have the
    drop mark the survey so a build skips it.

## 10. Plan

All work starts **after `i18n` is merged into `V3`**, on a branch per repository cut from the
merged line, with one PR per repository. AIUP docs come first in each:
- **Survey:** UC-008 (rebuild reporting schema) rewritten for per-survey, a new UC for rename, a
  new UC for the drop (3.7), and `entity_model.md`. UC-008 also takes the rule of 3.6, and UC-002
  BR-012 a sentence pointing at it.
- **Admin:** FR-027 and a new rename UC.
- **FHHS:** UC-005.
- **Author:** the reporting UCs, and UC-047 with FR-065 for the delete (3.7).

| Step | Repo | Content | Done when |
|---|---|---|---|
| 1 | Survey | Q-1 fixture; docs; `report_schema` column; ETL parameterized (3.3); schema creation; `fact_respondents` view; greenfield V002/V008 rewrite; `migration-v3` drop migration; build endpoint with `?survey=`; rename endpoint; drop endpoint (3.7); `dim_question`, `dim_item` and the two `fact_sections` columns with their fill and the three tests of section 8 (3.6, after `feature/multi-select-repeat` is merged) | Survey tests green with two surveys; `buildDockerImages.sh Survey` |
| 2 | FHHS | schema lookup; readiness; V0.0.6 both tracks; tests | greenfield **and** brownfield reports match |
| 3 | Admin | rebuild client passes the key; revoke grant; rename action and UC | apply of a second survey builds only it |
| 4 | Author | drop the `surveyreport.` qualifier and the cross-survey warning; schema-mirror; preview comment; Q-8's outcome, the impact preview and the reworded list-order caution (3.6); Delete Survey drops the schema first (3.7) | Author build, including the three-way language check |
| 5 | Umbrella | `CLAUDE.md`, the manual (gated by `check-properties.sh`), UC-005, `DeploymentScript.md`, the two research docs | `buildDockerImages.sh` incl. `Manual` |
| 6 | All | greenfield, brownfield, import-order and e2e runs of section 8 | all pass |

Steps 1 and 2 ship together: FHHS cannot read a schema Survey no longer creates, and Survey
cannot drop one FHHS still reads.
