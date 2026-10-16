# Default Reports: The Respondent Summary and the Administrator Report

> **Status (2026-10-03):** Research plan, re-verified against the code on 2026-10-03 (Survey `V3`
> ac602ab, Admin `V3` 990e62a, FHHS `V3` 8fb433c, Author `main`). Today a survey has **no reports
> unless somebody writes a report service for it**. Every report a respondent or an administrator sees comes from an external
> HTTP service listed in `survey.reports`, and the only such services that exist are FHHS's, written
> for the Family History Survey. A new survey built in Author therefore finishes on a page holding
> nothing but a "Generate PDF" button, and the Admin console cannot say how many respondents have
> finished it. This document proposes two **default reports** that every survey gets without writing
> code: a **respondent report**, which echoes back the answers the author marked for it, and an
> **administrator report**, which counts registered, started and finished respondents and shows,
> for the questions the author marked, how many respondents were presented each question and how
> many answered it. Section 4.3 suggests further metrics. Nothing here is implemented, and most of
> the decisions listed in section 7 are open.
>
> **Decided 2026-10-02 (section 2.4):** the respondent report is an ordinary row in `survey.reports`,
> the same table a custom report service is listed in. Author seeds the standard row(s) into every
> new survey, and the author can keep, remove, reorder or supplement them with custom services. The
> administrator report is not such a row, because the report contract is per-respondent.
>
> **Constraint (2026-10-03):** the `.elicit` export keeps its `reports:` lines, one per
> `survey.reports` row, whatever Author does to model them (section 2.4).
>
> **Required (2026-10-03):** a survey with no `survey.reports` rows shows no "Generate PDF" button
> and sends the respondent straight to its post-survey URL (G-14, D-16, phase 1).
>
> **Scope:** the reports are for new, generic surveys. FHHS is out of scope. It appears only as the
> existing example of a survey-specific report service (section 1.1), and nothing proposed here
> replaces or changes it.

## Overview

The plumbing for reports already exists and works. Survey's `ReportView` and Admin's
`ReportingService` both loop over `survey.reports`, POST a `ReportRequest` to each URL, and render
the `ReportResponse{title, innerHTML, pdf}` that comes back. Survey's request carries
`{id, language}`; Admin's carries `{id}` only (G-13). Both apps can then merge the responses into
one PDF. What is missing is **content**: there is no report service that works for an arbitrary
survey, and no way for an author to say which questions matter.

Most of what the two reports need is already in the `survey` schema:
- `survey.respondents` records who was registered, when they first logged in and when they finished.
- `survey.answers` holds a row for every question the rules made visible to a respondent, plus the
  value they gave.

Four things are missing:
- **A mark.** Nothing on a question says "important enough to report". Reporting tags (section 1.4)
  serve a different purpose.
- **A clean definition of "presented"** for respondents who have not finished, because answer rows
  are created before the page is shown (section 1.3).
- **A clean definition of "answered"** for defaulted questions and false checkboxes (section 1.3).
- **A home for the code.** C-009 keeps analytics dashboards out of Survey (section 4.4).

Terminology used in this document:
- A **default report** is one Elicit ships and every survey can use without a custom service.
- A **report mark** is the author's statement that a question, at a given placement, belongs in a
  given default report.
- A respondent was **presented** a question when that question was shown on a page the respondent
  reached.
- A respondent **answered** a question when they saved a value for it.
- The **response rate** of a question is answered ÷ presented.

## 1. What exists today

### 1.1 The report contract

| Piece | Where | What it does |
|---|---|---|
| `survey.reports(id, survey_id, name, description, url, display_order, report_key)` | Survey `V001:816` (V2 upgrade track `migration-v3/V001:467`), `report_key` in `V015:25` of both tracks; unchanged since | One row per report service, listed per survey. `url` is nullable; `(survey_id, name)` is unique |
| `ReportView` (`/report`) | Survey `flow/ReportView.java:56,100-121,126-133,136-142,173-230` | Shown after Finish, and on re-login by an inactive respondent (`MainView.java:142-145,155-158`, and the fresh-login path `:235-241`). One `ReportCard` per report, a "Generate PDF" button, and Next → `surveys.post_survey_url`. Sends `ReportRequest(id, language)` (`:175`) and hands `rpt.url` to `new URI(...)` unchanged (`:177-179`): no base URL, no timeout |
| `ReportingService.printReports` | Admin `service/ReportingService.java:185,194,267` | The same loop, run from the search grid for a respondent whose status is Finished (`SearchView.java:682-683`). Sends `ReportRequest(id)` only (`report/ReportRequest.java:56`); no timeout either |
| `PDFService`, `/api/pdf/download` | both apps | Merge each report's `PDFDocument` into one PDF |
| `survey.post_survey_actions`, `respondent_psa` | Survey `V001:834,850` | Fire-and-retry POSTs of `{"id"}` after Finish. These are not reports |

FHHS plugs in by being listed in `survey.reports`: `/pedigree/report`, `/proband/report` and
`/casummary/report` each return the contract above. They read the **star schema**, not the
answers: `CancerHistoryRepository` selects tag columns from `<report_schema>.fact_sections_view`
for the respondent (`:199-202`), resolving the schema name from `survey.surveys.report_schema` on
every request (`FamilyHistorySurveyCheck.java:70-78`, FHHS UC-005 BR-006). That works because
`finalize()` runs the per-respondent ETL before `ReportView` calls the services. FHHS's request
type has only `id`; it ignores the language Survey sends. **A
default report plugs in the same way**, through the contract (decided 2026-10-02, section 2.4).
Section 4.4 explains why it should not read the star schema. It then needs no new rendering, PDF
or Admin wiring, and an author can list it, reorder it or remove it alongside a custom one.

How the rows are managed today:
- **No module has a screen for them.** Nothing in Survey, Admin or Author lists, edits, reorders or
  deletes a `survey.reports` row. Rows arrive only by `.elicit` import (Admin
  `SurveyDefinitionImportService.insertReport`, `:914-930`) or by SQL. The only documented edit is
  the manual `UPDATE survey.reports SET url = …` that fixes the host after a deployment
  (`DeploymentScript.md:269-273`).
- **Update is an upsert by `report_key`.** Applying a new version of a survey matches rows by key,
  updates `name`, `description`, `url` and `display_order` in place, and inserts unknown keys
  (`SurveyDefinitionUpdateService.upsertReport`, `:1203-1236`). The file wins for every row it
  carries.
- **Update never deletes.** A row whose key is no longer in the file is left alone (class comment,
  `:74-75`); `reports` is a Type 1 table with no `effective_to`, so the retire path does not apply
  either. Removing a report in Author therefore does not remove it from a site that already has it
  (G-12).
- **Both callers order by `display_order`** (Survey `Survey.java:91-92`, Admin `Survey.java:196-198`)
  and the card heading is the row's `name`, translated by `report_key`
  (`ContentTranslator.java:285`), not the `title` the service returns.

Author does not model `reports` or `post_survey_actions` yet. Its exporter and importer pass the
rows through as raw SQL (`SurveyDefinitionExporter.java:95-96`, `SurveyDefinitionImporter.java:487-515`),
so an imported FHHS file round-trips its three rows, but an Author-built survey always exports zero
rows. (`Author/docs/requirements.md:209-212` and `entity_model.md:386` say the tables are "exported
with zero records", which is true only of an Author-built survey; the wording goes when the tables
are modeled.) The only other code that knows the table is `SurveyService.java:208`, which deletes
the rows with the survey, and `TranslatableFields.java:44`, which already lists a report's name and
description as translatable; `TranslationService.java:379` notes that "Author has no report
entity". An Author-built survey therefore cannot even *list* a report today. This is a prerequisite
for both reports.

### 1.2 The respondent lifecycle

`survey.respondents` (`V001:61-77`) carries `created_dt`, `first_access_dt`, `finalized_dt`,
`active` and a `logins` counter. There is no last-activity timestamp and no session or login
history (nothing like `presented_dt`, `last_activity` or a page-view stamp exists anywhere in
Survey). Status is derived the same way in three places: the `fact_respondents` trigger, the ETL's
`fact_respondents` view (`Sql.java:829-842`), and Admin's `survey.status` view (`V0.0.7:28-47`).

| Status | Rule |
|---|---|
| Not Started | `first_access_dt` and `finalized_dt` both null |
| In Progress | `first_access_dt` set, `finalized_dt` null |
| Finished | `finalized_dt` set |

There are two consequences for the reports:
- **"Registered" is not a status of its own.** Every row is registered, so Registered = Not
  Started + In Progress + Finished. The funnel the administrator report needs is
  registered → started → finished.
- **There is no withdrawn or expired state.** Finish runs `ReviewView:153` →
  `QuestionService.finalize` (`:255`) → `setActiveFalse` (`:275-281`), a native UPDATE that sets
  `active = false, finalized_dt = CURRENT_TIMESTAMP` without checking whether `finalized_dt` was
  already set. (`AccessCodeService.deactivate()`, `:272-285`, does the same but is called only by
  a test.) Any future "withdraw" built on either would be counted as Finished. Admin's
  `Status.java:239` javadoc already promises "invited/started/completed/expired", which do not exist.

There are also **two definitions of duration**: SQL uses `finalized − created`, while
`Respondent.getElapsedTime()` uses `finalized − first_access`. The report must pick one (section 7,
D-6).

### 1.3 What an answer row proves

Answer rows are created **eagerly**. `QuestionManager.init` builds a row for every unconditional
question in the survey on first entry, and again on each Next/Previous
(`QuestionManager.java:336-346`, `buildInitialAnswers :698-719`; called through
`QuestionService.init` from `MainView:232`, `SectionView:662,717` and `ReviewView:188`).
Rule-gated questions get a row only when a SHOW or REPEAT rule fires (`buildDownstreamQuestions
:803`, SHOW `:822-853`, REPEAT `:855-872`). From this follows:

| Fact | Meaning |
|---|---|
| Row exists, `deleted = false` | The question was **eligible** under the rules. This does not prove the page was shown |
| `text_value` not null, `saved_dt` set | Saved a value, **except**: a question with a `default_value` gets `saved_dt` at creation (`Answer.java:451-455`), so it looks answered whether or not the respondent touched it |
| `text_value` null | Unanswered, **except**: a CHECKBOX set to false is stored as NULL (`QuestionService.java:304-309`) **and still gets `saved_dt`** (`:302`), so for a checkbox neither column separates "no" from "no answer" |
| No row | Hidden by the rules |
| `deleted = true` | Was eligible and then hidden by a changed upstream answer. These rows are **purged on Finish** (`removeDeleted`, `:2379-2388`, from `QuestionService.finalize:258`), so no trace remains |

For a **Finished** respondent, eligible equals presented, because finishing requires passing through
every page and the review. The per-question metrics are therefore sound if they are computed over
finished respondents only. For **In Progress** respondents, the rows for pages they have not reached
already exist, so eligible overstates presented. Section 5 proposes the fix.

Section and step header rows have `question_id` null and carry the section or step name in
`display_text` (`QuestionManager:743`), with any token already filled. They are the natural
headings for the respondent report.
`display_text_local` and `display_language` (`V019:123-124`) mean the report can be rendered in the
language the respondent answered in. Select-type answers store `select_items.coded_value`,
comma-joined for multi-selects (`SectionView:222,263,300,317`), so the report must map each code
back to the item's (translated) `display_text`.

Repeats come in two kinds since 2026-10-02 (Survey #135). A **count** repeat numbers its instances
1..N. A **per-item** repeat, driven by a MULTI_SELECT or CHECKBOX_GROUP, numbers each instance by
the selected item's 1-based position in its select list (`QuestionManager:1441-1455`), so the
instances are **not contiguous**: selecting the 2nd and 5th items gives {2, 5}, and deselecting one
removes only that instance (`:1916-1928`). A section repeat stamps the instance on the header row
and on every question row of that copy (`section_instance`); a question repeat stamps
`question_instance`. `repeatedItem()` (`:1464`) maps an instance back to its item. The respondent
report must therefore group by instance as found, never by 1..n, and head each instance by its own
header row or item text.

### 1.4 Reporting tags are not report marks

Author's reporting tags are `survey.dimensions`, `ontology` and `metadata`, edited at
`survey/:id/reporting` (`ReportingView.java:66`; Author FR-023..026 "Assign / Reassign Ontology
Tag", "Create Ontology & Dimension Entries", "Preview Dimensional Impact", UC-021..024). They attach a tag to a question, a section
placement or a steps_sections placement, and Survey's ETL turns each tag into a `dim_*` table and a
`<tag>_key` column on `fact_sections`. They answer "which columns does the analytic star schema
have". They do **not** answer "what should a respondent see on their summary" or "which questions
does a coordinator monitor":
- Reusing them would force every reported question into the star schema, which costs a column and
  a retroactive rename warning (C-011/C-012).
- Reusing them would force every star-schema column onto the respondent's summary, which may include
  derived or internal codes.
- The audiences differ: a statistician, a respondent and a study coordinator.

The proposal (section 2) is therefore a separate mark. Author shows it on the same question rows,
next to the tag badges, so an author sees both at once. Because Author's wording currently
describes the tags as building "the report", section 2.3 proposes renaming them.

### 1.5 Gaps found

| # | Gap | Effect on the reports |
|---|---|---|
| G-1 | No report mark on any question | Nothing to echo back or to monitor |
| G-2 | Author exports `reports` with zero rows | An Author-built survey cannot list any report, default or custom. The Reports page that closes this (section 2.2) is also where the default row is seeded (section 2.4) |
| G-3 | `fact_respondents` triggers and ETL filtered on `survey_id = 1` | **Fixed in Survey on 2026-10-02** (Survey #136, UC-008, FR-029): every survey has its own schema, `report_<slug of the survey name>` (`etl/ReportSchemaNames.java:38`), recorded on `surveys.report_schema` (`V021:22-33`), built by `POST /api/etl/build?survey=<key>`, renamed by `POST /api/etl/schema/<key>/rename` and dropped by `DELETE /api/etl/schema/<key>` (behind `elicit.etl.drop.enabled`). No `survey_id = 1` literal remains; `fact_respondents` is a view (`Sql.java:829`); `fact_sections` gained `question_key`/`item_key` (`:154-160`) for per-item repeats |
| G-4 | Eager answer rows | "Presented" is only sound for finished respondents |
| G-5 | Defaulted questions get `saved_dt`; a false checkbox is NULL and also gets `saved_dt` | "Answered" is wrong for both types, and no column at all distinguishes an unchecked box from an untouched one |
| G-6 | Deleted rows purged on Finish | Cannot report "shown, then hidden by a changed answer" |
| G-7 | No last-activity timestamp | Cannot tell an active In Progress respondent from an abandoned one |
| G-8 | No withdrawn/expired state; `setActiveFalse()` sets `finalized_dt` unconditionally | A future withdraw would be counted as Finished |
| G-9 | Two definitions of duration | Admin and the star schema would disagree. **Settled 2026-10-02:** the star schema's `fact_respondents` view uses `finalized_dt - first_access_dt` (`Sql.java:840-842`), the same as `Respondent.getElapsedTime()` (`Respondent.java:104-106`; Survey UC-008 BR-010); D-6 should keep to it |
| G-10 | Admin has no dashboard FR/UC, though Admin `vision.md:42-45` lists "progress monitoring dashboards" as in scope | The administrator report needs requirements before code |
| G-11 | Author calls the star-schema tags "reporting tags" and their columns "report columns", and tells authors to "tag what the report needs" | Once default reports exist, an author will expect tagging a question to put it on a report (section 2.3) |
| G-12 | Applying a survey update never removes a `reports` row whose `report_key` has left the file (`SurveyDefinitionUpdateService.java:74-75`) | An author who drops the default report, or a custom one, and republishes does not remove it from a deployed site. The row keeps running until someone deletes it by SQL (section 2.4, D-13) |
| G-14 | `ReportView` shows "Generate PDF" whatever the row count (`ReportView.java:100-121`) and shows Next only when `post_survey_url` is set (`:136-142`); Finish (`ReviewView.java:155`) and re-login (`MainView.java:142-158,235-241`) always land on it | A survey with no `survey.reports` rows ends on a page holding a PDF button that produces an empty PDF, and with no post-survey URL a page holding nothing at all. **Required 2026-10-03:** with no rows, hide Generate PDF and send the respondent straight to the post-survey URL (D-16) |
| G-13 | Admin's `ReportRequest` carries only `id` (`report/ReportRequest.java:56`, `ReportingService.java:267`); Survey's carries `id` and `language` (`ReportView.java:175`) | A default respondent report printed from Admin does not know which language to render. FHHS never read the field (section 2.4, D-14) |

## 2. How an author marks what to report

### 2.1 Proposed model

The mark is one new table, which travels in the `.elicit` file like `metadata` does:

```
survey.report_items
  id, survey_id
  report_type        RESPONDENT | ADMINISTRATOR
  question_id | sections_question_id | steps_sections_id   -- exactly one, durable ids
  label              -- optional; replaces the question text on the report
  display_order
  report_item_key    uuid   -- cross-instance identity, like the other *_key columns
```

The design reasons:
- **The same placement choice as `metadata`.** A question marked at `question_id` is reported
  wherever it appears. One marked at `sections_question_id` is reported only at that placement. A
  `steps_sections_id` mark reports a whole section, which is the short way to say "echo this entire
  page". Author's `Metadata` entity (`Metadata.java:44-67`) is the model: exactly one of the three
  columns set (UC-021 BR-001, not enforced by a constraint), exported as seven fields without
  `survey_id` (`SurveyDefinitionExporter.java:99`). `report_items` follows it field for field.
- **Two report types, not one flag.** What a respondent should see back and what a coordinator
  monitors overlap but are not the same set. A consent question is monitored but not echoed. A
  "your goals" free-text field is echoed but not monitored, because its content cannot be
  aggregated.
- **A label** lets the summary say "Your height" where the question says "What is your height in
  centimeters?". It is translated through `survey.translations` (a new `element_type`), as the
  report name already is (`V019:74`).
- **Durable ids** keep a mark across question revisions, the same rule as `metadata`.

The rejected alternatives:
- A boolean column on `questions`. It cannot express placement, it cannot be per-report, and it
  versions the question when the mark changes.
- Reusing reporting tags. See section 1.4.

### 2.2 Author UI

The UI follows the reporting-tags pattern rather than inventing a new one:
- A **Reports** page per survey (`survey/:id/reports`) with two tabs, Respondent and Administrator.
  Each tab lists the marked items in report order, which can be dragged to reorder.
- A mark glyph on `QuestionRow` beside the `TagBadge`, with a menu entry to mark or unmark the item
  for either report.
- The same page models the `survey.reports` rows themselves (closes G-2): name, description, URL,
  and display order, in one list that can be dragged to reorder. A new survey starts with the
  standard row(s) already in the list (section 2.4). A default row is badged as built-in so an
  author can tell it from a service somebody wrote, but it is edited, moved and removed like any
  other row. Adding a row with a custom URL is how a site-specific service such as FHHS's is listed.
- A preview. Author already runs a preview Survey (`author.preview.survey.url`,
  `PreviewSurveyClient`) whose respondents and answers are deleted with the survey, so the preview
  is the real built-in service (section 3.3) rendered for a preview respondent, opened from the
  Reports page. There is no second renderer from `questions.sample`; that column is authoring-only
  (UC-011 BR-005) and feeds the token samples, not a report.

### 2.3 Rename reporting tags to analysis tags

Author's tag screens describe the star schema as *the* report. Once the Reports page in section 2.2
exists, "report" would mean two unrelated things in the same tool:
- Tagging a question for the star schema, and
- marking a question for the respondent or administrator report.

The wording that causes the confusion is in `Author/src/main/resources/vaadin-i18n/translations.properties`
(line numbers as of 2026-10-03):

| Key | Today |
|---|---|
| `reportingView.intro` (:487) | "A reporting tag names a column of **the site's report**." Unchanged by d4fda83, which reworded the impact strings to "the survey's own reporting schema" |
| `guideView.reportingTags.p1` (:262) | "names a column of **the report a site builds**" |
| `guideView.designing.p2` (:260) | "**tag what the report needs**". This is the most misleading: under this plan, what a report needs is a report mark |
| `reportingView.grid.reportColumn` (:493), `elementTagsDialog.grid.reportColumn` (:533) | "Report column" |
| `surveyEditorView.reporting` (:59), `designer.menu.reportingTags` (:280), `reportingView.pageTitle` / `.heading` (:481-482), `tagBadge.title` (:377), `elementView.fact.reportingTags` (:467), `guideView.designing.reportingTags.term` / `.desc` (:256-257), `guideView.reportingTags.h` (:261) | "Reporting…", "Reporting tags…", "Reporting tags" |

Those are the headline strings. The word runs through whole families of keys that a rename must
sweep together, each with its `translations.context.properties` entry and its `es-419` and `ar`
row: the rest of the `reportingView.*` block (:483-526, including `impact.reportColumns` and the
new `impact.perItemColumn(s)`), `elementTagsDialog.*`, `tagAssignmentDialog.*` (`.reportedValue`,
`.impact.text`), `tagDialog.*` (`.preview.text`), `elementDialog.*` (:595-601),
`validation.tag.*` (:126-144), `reporting.scope.*` / `.value.*` / `.where.*` (:977-982),
`error.tag.column.*` (:995-997), `exportDialog.retroactiveReminder` (:170) and
`aiDraftView.whatComesBack.p2` (:206). Outside the bundle: the manual section "Tagging for
reporting" (`docs/manual/elicit-author-manual.tex:432-458`), the in-app guide
(`SurveyModelGuideView.java:94-99`) and `docs/ai/AUTHORING_FROM_AI.md:206`.
`i18n/TRANSLATION_REQUEST.md` is regenerated by `TranslationRequestGeneratorTest`, which fails on
drift and says what to copy.

The star schema exists for **analysis**: statisticians, downstream tools, and the BinaryFilter
faceted browser (`faceted_exploration.md`). The proposal is:
- Call them **analysis tags**, on an **Analysis…** page.
- Change "Report column" to "Column".
- Reserve "report" for something a person reads: the respondent report, the administrator report,
  or a custom report service such as FHHS's.

The rename is limited to what people see:

| Change | Leave |
|---|---|
| The strings above and their `es-419` and `ar` translations, which means regenerating the translation request | `survey.dimensions` / `ontology` / `metadata` |
| The Author guide and the Author manual's chapter on tags | the schema names: `surveyreport` (shared `dim_date`/`dim_status`), the `report_` prefix of each survey's own schema and `surveys.report_schema`; deployed databases, grants and FHHS's queries name them |
| Author FR-023..026 and UC-021..024 titles and prose | `ReportingNames` and other class names that authors never see |
| The `survey/:id/reporting` route, renamed to `survey/:id/analysis` | the `.elicit` table names |
| Whether "reporting schema" follows is decision D-11. It is now a user-facing term in three modules: Admin's console (`translations.properties:198,202,536-554`, since f19652a), Admin FR-027 / FR-037 / UC-030, Survey UC-008 / 010 / 011, the installation manual and `DeploymentScript.md:113-154`. Admin's `reporting.error.*` strings are about report services, which is already the right meaning | Survey's `etl` package, `/api/etl/build` and `/api/etl/schema` |

The rename belongs in phase 3 (section 8), before or together with the Reports page. That way the
two meanings of "report" never appear side by side in a release.

### 2.4 Default reports are rows in `survey.reports`

Decided 2026-10-02. A default report differs from FHHS's report services only in who wrote the
service. It is listed in the same table, called through the same `ReportRequest`/`ReportResponse`
contract, ordered by the same `display_order`, named by the same translated `name`, and printed by
Admin through the same loop. Nothing in `ReportView` or `ReportingService` knows that a row is a
default. The consequences:

- **The `reports:` lines stay in the `.elicit`.** The export keeps carrying one `reports:` line per
  `survey.reports` row, with the six fields `id, report_key, name, description, url, display_order`
  that Author writes (`ElicitFormat.java:94`) and Admin's `insertReport` reads (`:914-930`): the
  FHHS file's three, a new survey's seeded default, and any custom service. Modeling the table in
  Author changes where the rows come from, not the lines. Today the exporter passes them through
  untouched (`SurveyDefinitionExporter.java:95-96`), which is why an imported survey round-trips
  its report services; once they are entities the exporter must still write every row, so a survey
  imported into Author, edited and re-exported never loses a report. `report_items` is an added
  table, not a change to `reports`.
- **Seeding.** Author's Reports page (section 2.2) pre-populates the standard row(s) when a survey
  is created. Today that is one row, the respondent report of section 3; a future standard
  per-respondent report would be a second seeded row. The rows travel in the `.elicit` like every
  other authored table and are inserted by Admin's apply.
- **The author decides.** The seeded rows are a starting point, not a fixture. An author can keep
  them, delete them, move them above or below custom rows, or add rows of their own. *Replacing*
  the default is deleting the seeded row and adding a custom one. *Supplementing* it is keeping
  both. A survey with nothing marked for the respondent report can simply drop the row. **A
  survey with no `survey.reports` rows has no report page** (required 2026-10-03, G-14, D-16):
  Finish takes the respondent straight to `surveys.post_survey_url`, and "Generate PDF" is never
  shown, because there is nothing to put in the PDF. Today the page is shown regardless, with the
  button and, when the URL is set, a Next button.
- **Author is the only editor.** The rows are authored content, so there is no Admin page for them.
  A site changes a report by republishing the survey. This keeps one source of truth, and the
  `.elicit` already carries the rows. (An Admin editing page was considered and rejected: it would
  make the rows site-local and force a merge rule on every re-apply.)
- **The file wins on re-apply.** Admin's update already upserts by `report_key` (section 1.1), so
  a republished survey restores the authored name, URL and order. This is the intended behavior,
  not a gap.
- **Removal must work.** The one missing piece is G-12: the update leaves a row alone when its key
  has gone from the file, so deleting the default report in Author does not yet delete it on a
  site. Phase 3 closes this (D-13).
- **The built-in service's URL cannot name a host.** FHHS's rows carry an absolute, site-specific
  URL (`http://host.docker.internal:8082/proband/report`) that the deployment procedure patches by
  hand (`DeploymentScript.md:269-273`). A row seeded in Author knows nothing about the site that
  will apply it, and the default service lives in Survey itself. How the row expresses that is
  D-12; the leaning is a relative path that each caller resolves against Survey's base URL. Admin
  has that already as `elicit.survey.url` (`application.properties:44`, used by
  `ReportingSchemaRebuildClient.java:99-123` and probed by Connection Checks); Survey has no
  base-URL setting, and `ReportView.java:177-179` hands `rpt.url` to `new URI(...)` unchanged.
  Neither caller sets a connect or read timeout (research question 9).
- **Admin's print carries no language.** Survey sends `ReportRequest(id, language)`; Admin sends
  `ReportRequest(id)` (G-13). FHHS never looked at the field, but the respondent report must know
  which language to render. Decision D-14.

The administrator report (section 4) is **not** a `survey.reports` row. The contract posts one
respondent's id (`ReportView.java:175`, `ReportingService.java:267`) and both callers run for one
finished respondent, so a survey-wide aggregate does not fit it. It stays an Admin view (section
4.4).

## 3. The respondent report

### 3.1 Content

| Part | Source |
|---|---|
| Title, survey name, completion date | `surveys`, `respondents.finalized_dt`, in the respondent's language |
| Brand header | the mounted brand, as the rest of Survey |
| Body: marked items grouped by step then section, in survey order | `report_items` joined to the respondent's non-deleted `answers` |
| Repeat instances (e.g. one block per household member, or one per language selected) | `section_instance` / `question_instance` as found, which are non-contiguous for a per-item repeat (section 1.3); a section instance is headed by its own header row, whose `display_text` already carries the filled token; a question instance by its own `display_text` |
| Value | `text_value` mapped by type (section 3.2) |
| Optional closing text | an author-written HTML block per survey, translated |

### 3.2 Rendering rules per question type

| Type | Rendered as |
|---|---|
| RADIO, COMBOBOX | the selected item's translated `display_text`, never the `coded_value` |
| MULTI_SELECT, CHECKBOX_GROUP | a list of the selected items' labels |
| CHECKBOX | Yes / No. Because of G-5, NULL renders as No |
| INTEGER, DOUBLE | locale-formatted number, plus a unit if the label carries one |
| DATE_PICKER, DATETIME | locale-formatted date |
| TEXT, TEXTAREA, EMAIL | the text, escaped |
| HTML | not markable (it is informational, with no value) |
| PASSWORD | not markable, and the Author UI refuses the mark |

A marked question the respondent did not answer, or never saw, is rendered as "Not answered" or
omitted, depending on decision D-3.

### 3.3 Where it runs

The respondent report is a **built-in report service inside Survey**, for example
`POST /api/reports/respondent-summary`, implementing the existing `ReportRequest`/`ReportResponse`
contract:
- Survey already holds the answers, the translations, `PDFService` and the brand, so nothing new has
  to cross a module boundary.
- It produces `innerHTML` for the `ReportCard` and a `PDFDocument` for the merged PDF.
- Admin's "print reports" picks it up automatically, because it walks the same `survey.reports` rows.
  Admin prints only a Finished respondent (`SearchView.java:682-683`) and today sends no language
  (G-13, D-14), so the service must fall back to the respondent's `display_language` when none
  arrives.
- Whether `ReportView` reaches it over HTTP to itself or in-process is research question 9 (D-12).
- C-009 forbids *analytics dashboards* in Survey. A per-respondent echo of their own answers is not
  one.

Every new survey gets the row: Author seeds it on the Reports page, and the author deletes it if the
survey has nothing to echo back (section 2.4, D-1). If the row stays with no respondent marks, the
service renders the title, completion date and closing text only, and D-3 decides whether it says so.
If the author deletes it and lists no other service, the survey skips the report page altogether and
Finish goes straight to the post-survey URL (G-14, D-16).

### 3.4 Disclosure

The summary is reachable again by logging in with the access code (`MainView.java:142-158,235-241`),
so the summary is exactly as private as the access code. Marking a sensitive question is therefore
an authoring decision with a consequence. The Author UI should say so when marking a question in
the Respondent report. Every generation is a PHI read and must be logged. Admin NFR-005 (PHI Access
Auditing) states the requirement; it is still Open, so there is no implemented precedent to copy.

## 4. The administrator report

### 4.1 Participation (unmarked, every survey)

| Metric | Definition |
|---|---|
| Registered | count of `respondents` rows |
| Started | `first_access_dt` not null |
| Finished | `finalized_dt` not null |
| Not started / In progress | the two remaining states |
| Start rate | Started ÷ Registered |
| Completion rate | Finished ÷ Started |
| Overall yield | Finished ÷ Registered |

Every count is shown **per department** (Admin already scopes every query by the user's departments)
and **over time**. The time view is a cumulative line of registered, started and finished against
date, plus counts per week, so a coordinator can see recruitment and the lag between them.

### 4.2 Marked questions (Administrator mark)

Per marked question, computed over **finished respondents** until section 5 lands:

| Metric | Definition |
|---|---|
| Presented | distinct respondents with a non-deleted answer row for the question (per instance for repeats, see D-4) |
| Answered | of those, `text_value` not null, excluding default-only saves once G-5 is fixed |
| Response rate | Answered ÷ Presented |
| Reach | Presented ÷ Finished. This is how often the rules show the question at all, and it separates "people skip this" from "few people get here" |
| Distribution | select types: count and % per option, in option order; numbers: n, min, quartiles, max; dates: min, max, counts per month; free text: count only, never content |

Reach and response rate are the user's "how many were presented, and what were the response rates",
split into the two quantities that answer different authoring questions.

### 4.3 Suggested further metrics

They are grouped by whether they are available from today's data.

**Available now:**

| Metric | Why it is useful | Source |
|---|---|---|
| Time to start (created → first access) | Whether invitations work; when reminders should go | `respondents` |
| Time to complete, median and IQR | Respondent burden; detects a survey that is too long | `respondents` (after D-6) |
| Sessions to finish (`logins` distribution) | Whether people finish in one sitting | `respondents.logins` |
| Language mix | Which translations are used; where review effort pays | `answers.display_language` |
| Repeat-instance counts (e.g. household members per respondent) | Size of the repeating parts of the survey; data volume | `answers.section_instance` |
| Never-presented questions | Dead rule branches, an authoring QA signal | marked or all questions with zero answer rows among finished respondents |
| Post-survey action health | Failed or pending integrations that need attention | `respondent_psa` status, tries, error_msg |
| Email delivery backlog | Invitations queued but not sent | Admin's unsent-message queue |
| Report service failures | A broken custom report service | Admin FR-025 connection checks |

**Need new data (section 5):**

| Metric | Why it is useful | Needs |
|---|---|---|
| Drop-off by step: where In Progress respondents stopped | The single most actionable survey-design metric | per-page first-view timestamp (G-4) or last-activity (G-7) |
| Stalled In Progress (no activity in N days) | Whom to remind | last-activity timestamp (G-7) |
| Time per step/section | Which page is the burden | per-page view timestamps |
| Answer changes ("shown, then hidden by a changed answer") | Confusing branching questions | keep deleted rows, or an event log (G-6) |
| Withdrawn / expired counts | An honest funnel | a status distinct from Finished (G-8) |
| Device or browser class | Mobile-layout problems | a user-agent class on the respondent, if acceptable |

**Deliberately not suggested:** straight-lining or speeder detection, cross-survey comparison, and
free-text analytics. These belong to downstream analysis tools (Author `vision.md:115`) and to the
star schema and BinaryFilter work in `faceted_exploration.md`.

### 4.4 Where it runs

The administrator report runs **in Admin**, as a view of its own and not as a `survey.reports` row.
The report contract is per-respondent (section 2.4), so there is nothing for a survey-wide report to
plug into. Beyond that, three reasons put it in Admin:
- Admin's vision already names progress monitoring as in scope (G-10).
- Admin has department scoping.
- Admin is the console the coordinator uses.

It has two outputs:
1. **A participation view** (new route; not "overview", which Admin already uses for FR-020 /
   UC-020 "View System Overview"; the name is D-15), with a survey picker, a department filter, and
   a date range. It shows the participation tiles and funnel (4.1), the
   timeline, and one card per marked question (4.2). Admin has no charting dependency. A small set of
   charts (funnel bar, cumulative line, per-option bar) is the decision in D-7.
2. **An exportable form**: a PDF of the same content through Admin's existing `PDFService`, and a
   CSV of the aggregate tables. Aggregates only; row-level export stays the existing per-respondent
   `ELICIT_EXPORT_V2`.

Both default reports query the OLTP tables (`respondents`, `answers`, `report_items`) directly, not
the star schema, unlike FHHS (section 1.1). G-3 was the first reason when this was written; it was
fixed on 2026-10-02 (Survey UC-008) and a new survey now gets its own schema on apply. Three
reasons remain:
- **Marks are not tags.** The star schema has a column only for a *tagged* question, while a report
  mark is independent of tagging (section 1.4).
- **The star schema keeps values, not presentation.** It stores the value or tag constant per
  step/section instance. It does not keep the question text, the option labels in the respondent's
  language, or display order, and every one of those is something the reports need.
- **The star schema holds only answered rows of finished respondents.** The ETL's row filter is
  `saved_dt IS NOT NULL AND text_value IS NOT NULL AND finalized_dt IS NOT NULL` and `deleted != true`
  (`Sql.java:423-458`), so a question that was presented and left blank is not there at all, and
  neither is an In Progress respondent. "Presented" cannot be computed from it.

With G-3 fixed, the star schema is a reasonable second source for cross-respondent
analysis of tagged questions, but not for these two reports. The aggregate queries
are small (counts grouped by status, question and option), and an index on
`answers(question_id, respondent_id)` should be measured before anything heavier is considered
(NFR target to be set in D-8).

### 4.5 Disclosure

- **Small-cell suppression:** when a department filter, an option count or a date bucket gives a
  count below *k* (default 5, configurable), show "< k". Without it, a narrow filter on an aggregate
  identifies a person.
- Aggregates are not PHI reads in themselves. Drilling from a count to the respondents behind it
  would be a PHI read, and is out of scope for the first version (D-9).

## 5. Data the plan needs captured first

Every Survey migration below goes in both migration tracks, `db/migration` and the V2 upgrade
track `db/migration-v3`, or the upgrade path fails on the pending migration.

| Change | Module | Closes | Notes |
|---|---|---|---|
| `answers.presented_dt`: set when the page holding the row is first rendered | Survey | G-4 | Makes "presented" exact for In Progress respondents and gives drop-off and time-per-step. Pre-existing rows of finished respondents can be back-filled from `created_dt` |
| `respondents.last_activity_dt`: set on each save or navigation | Survey | G-7 | Cheap; one column |
| Distinguish a saved value from a default | Survey | G-5 | For example, set `saved_dt` only on an explicit save, and render the default without stamping |
| Store an explicit `false` for CHECKBOX | Survey | G-5 | A data-semantics change. Check ETL and FHHS readers for `IS NULL` assumptions first |
| A withdrawn/expired state that does not set `finalized_dt` | Survey + Admin | G-8 | Only needed when a withdraw feature is built. Record the constraint now |
| Keep deleted rows (or log the event) instead of purging on Finish | Survey | G-6 | Optional; storage cost versus the "answer changed" metric |
| Model `reports` and `report_items` in Author and the `.elicit` format; the Reports page seeds the default row into every new survey | Author, Survey (migration), Admin (apply) | G-1, G-2 | The `reports:` lines keep their six fields and every row is exported (section 2.4). Format stays `ELICIT_SURVEY_EXPORT_V1` (`SurveyDefinitionFileFields.java:48` says additions never bump it). `report_items` exports like `metadata` (section 2.1) and goes into `ElicitFormat.TABLES` after `metadata`, before `translations` |
| Teach the three `.elicit` parsers the `report_items` table | Admin (import and update), Author (import) | question 7 | None of them skips an unknown table (section 6); a file carrying `report_items` fails on an older build. No shim: V3 has never been released |
| Widen `translations_element_type_ck` for `report_items` labels | Survey | question 8 | Both tracks. The closing text can be a new `field` on the existing `reports` type with no migration |
| Send `language` in Admin's `ReportRequest` | Admin | G-13 | Per D-14. `ReportRequest.java:56` has `id` only; Survey's has had `language` since V019 |
| Correct Author's "exported with zero records" wording (`requirements.md:209-212`, `entity_model.md:386`) and `AUTHORING_FROM_AI.md:206` ("omit the last five tables") | Author | G-2 | Falls out of modeling the tables |
| Remove a `reports` row on update when its `report_key` has left the file | Admin | G-12 | Delete or retire per D-13. Today's update never deletes (`SurveyDefinitionUpdateService.java:74-75`) |
| Resolve a relative report URL against Survey's base URL in both callers | Survey, Admin | — | Needed by D-12 if the relative-path option is chosen; Admin has `elicit.survey.url` already, Survey needs its own base URL |

## 6. Research questions

These need an answer from the code or a prototype before requirements are written:

1. **Does `presented_dt` belong on the row or the page?** Rows are rebuilt on each Next/Previous
   (`QuestionManager.init`). Find the one place a page is rendered for the first time and confirm it
   can stamp only its own rows.
2. **Default values.** Which existing surveys rely on `saved_dt` being set for defaulted questions
   (review, validation, the ETL `saved_dt IS NOT NULL` filter at `Sql.java:282-325`)?
3. **Checkbox false.** Which readers (ETL, FHHS, Admin export) treat NULL as false? Changing the
   storage is a migration plus a reader audit. *FHHS audited 2026-10-03:* its readers compare the
   string to `"true"` (`FamilyManager.java:85-87`, `RowConverter.java:67-159`,
   `FamilyMember.java:247,518-669,702-750`), so a stored `"false"` reads as false and nothing breaks.
   The open risk is the ETL: its `text_value IS NOT NULL` filter (`Sql.java:423-458`) would start
   emitting fact rows for unchecked tagged checkboxes. ETL and Admin export still to audit.
4. **Repeats.** For a question inside a REPEAT section, is "presented" counted per respondent or per
   instance? Prototype both on a household-style survey and see which one a coordinator reads
   correctly.
5. **Query cost.** Measure the per-question aggregate over a synthetic 10 000-respondent survey on
   the dev database before deciding between live queries and a materialized summary.
6. **Charts in Admin.** Vaadin Charts is commercial. Weigh it against a lightweight SVG renderer for
   three chart types, rendered server-side so the PDF and the screen match.
7. **The `.elicit` format.** *Answered 2026-10-03: no parser skips an unknown table.* Admin's
   import and update each dispatch on a hard-coded `switch (tableName)` whose `default` records
   `definition.unknownTable` and fails the apply (`SurveyDefinitionImportService.java:442-443,459-460`,
   `SurveyDefinitionUpdateService.java:424-425,447-448`); Author's importer fails with
   `error.import.unknownRecord` (`SurveyDefinitionImporter.java:160`). The format stays
   `ELICIT_SURVEY_EXPORT_V1` and all three parsers learn the table in the same release (section 5).
8. **Translations.** *Answered 2026-10-03.* `translations_element_type_ck` (`V019:74`, v3 track
   `:78`) allows `surveys, steps, sections, questions, select_items, relationships, reports`;
   `field` is an unconstrained `varchar(32)`. A label on `report_items` needs a migration that
   widens the constraint, in both tracks; the closing text rides on the existing `reports` type as
   a new field with no migration.
9. **Self-call or in-process.** `ReportView` would POST to Survey's own built-in service. Survey has
   no base-URL setting, neither report client sets a timeout, and a dev-mode Survey parks requests
   to itself while it restarts. Decide between resolving a relative path to a real HTTP call and
   dispatching a `builtin:` row in-process (D-12), and prototype the one chosen under dev mode.

## 7. Decisions to make

| # | Decision | Options | Leaning |
|---|---|---|---|
| D-1 | Is the respondent report on by default? | every survey gets the row / added when the first respondent mark exists | **Settled 2026-10-02:** every new survey gets the standard row(s) from Author's Reports page; the author deletes, reorders or supplements them (section 2.4) |
| D-2 | One mark table or two? | `report_items` with `report_type` / separate tables | one table |
| D-3 | Unanswered marked item on the summary | "Not answered" / omit | configurable per survey, default omit |
| D-4 | Repeat counting | per respondent / per instance | show both; per-instance as the primary figure |
| D-5 | Population for per-question metrics | finished only / everyone presented | finished only until `presented_dt` exists |
| D-6 | Duration | created → finalized / first access → finalized | first access → finalized, and fix the SQL to match |
| D-7 | Charting | Vaadin Charts / SVG / tables only in v1 | tables in v1, charts in v2 |
| D-8 | Freshness | live query / nightly materialized | live, if question 5 confirms the cost |
| D-9 | Drill-down from a count to respondents | v1 / later | later, with NFR-005 logging |
| D-10 | Small-cell threshold | fixed 5 / configurable | configurable, default 5 |
| D-11 | User-facing name for star-schema tags, and whether "reporting schema" follows | keep "reporting tags" / "analysis tags" / "data tags"; and (a) rename the tags only or (b) also call the schema the "analysis schema" in Admin's console, Survey UC-008/010/011, the manuals and `DeploymentScript.md` | "analysis tags", and (a): "reporting schema" names a database object (`report_<slug>`) operators see literally, and Author's tag strings already say "the survey's reporting schema" (section 2.3) |
| D-12 | How the seeded row names the built-in service | absolute URL patched per site after apply (as FHHS, `DeploymentScript.md:269-273`) / a relative path such as `/api/reports/respondent-summary` that `ReportView` and `ReportingService` resolve against Survey's base URL / a `builtin:` scheme dispatched in-process | relative path: Admin has `elicit.survey.url`, Survey needs one base-URL setting, and custom rows keep working unchanged. `builtin:` avoids Survey calling itself over HTTP with no timeout (question 9); revisit if the prototype parks in dev mode |
| D-13 | A `reports` row whose key has left the republished file | delete / retire with an `effective_to` / leave as today | delete: `reports` is a Type 1 table with no history, and leaving it running is G-12 |
| D-14 | Which language Admin's print asks the respondent report for | the respondent's answering language (`answers.display_language`) / the administrator's UI language / the survey's base language | the respondent's: Admin sends it in `ReportRequest`, and the service falls back to `display_language` when the field is absent (G-13) |
| D-15 | Name of the Admin view | "Survey Overview" / "Participation" / "Progress" | "Participation": Admin's FR-020 / UC-020 "View System Overview" already owns "overview" |
| D-16 | A survey with no `survey.reports` rows (required: no PDF button, straight to the post-survey URL) | the two edge cases: (i) re-login by a finished respondent: redirect again / show a completion notice; (ii) no rows **and** no `post_survey_url`: a completion notice with no buttons / refuse such a survey at apply | (i) redirect again, the same rule on every path to `/report`; (ii) a completion notice, since `post_survey_url` is nullable (`V001:46`) and Author cannot require it. Amend Survey UC-004 (Finish) and UC-005 (View Reports) |

## 8. Plan

Under AIUP, docs come before code in every module. Each phase starts with FR + `use_cases.puml` + UC
spec in the module it touches.

| Phase | Work | Modules | Verification |
|---|---|---|---|
| 0 | Answer the research questions in section 6; settle the D-table | — | this document updated |
| 1 | No `survey.reports` rows: hide "Generate PDF", send Finish and re-login straight to `post_survey_url`, completion notice when there is none (G-14, D-16); amend UC-004 and UC-005 first | Survey | the census sample (no reports) finishes on its post-survey URL; the Family History Survey (three rows) still shows its cards and PDF; a survey with neither shows the notice |
| 2 | Data capture: `presented_dt`, `last_activity_dt`, default/checkbox semantics (section 5), in both migration tracks | Survey | greenfield and brownfield (`resetDatabase.sh V2`) runs; `report_family_history_survey` unchanged for the Family History Survey |
| 3 | Rename reporting tags to analysis tags in Author's UI, guide, manual and docs (section 2.3, D-11); model `survey.reports` and `report_items` in Author, with the Reports page seeding the default row (section 2.4); teach the three parsers the table and widen the translations constraint (section 5); `.elicit` round-trip; Admin apply deletes unlisted `reports` rows (D-13); report URL resolution (D-12); Admin sends the language (D-14) | Author, Survey (migration), Admin | export, apply and re-export give identical files; a file without `report_items` still applies; a survey republished without the default row no longer lists it after apply; Author's three-way build check |
| 4 | Respondent report service in Survey; Author preview through the preview Survey (section 2.2) | Survey, Author | `samples/census-household-survey.elicit` (every type, every rule, a count repeat and a per-item repeat) with marks added through its generator, in en, es-419 and ar (RTL); PDF and card match; TC-001's `CensusMultilingualE2ETest` extended to open the report |
| 5 | Administrator report v1: participation view (D-15), marked questions, tables, PDF/CSV, suppression | Admin | counts reconcile with direct SQL on a seeded survey; department scoping; k-suppression |
| 6 | v2: charts, drop-off by step, stalled respondents, time per step | Admin | the same seeded survey with In Progress respondents at known steps |

The phases are ordered so that each one is useful on its own:
- Phase 1 alone fixes today's dead end for every survey without a report service.
- Phase 3 alone lets an author list a custom report.
- Phase 4 alone gives every respondent a summary.
- Phase 5 runs on finished respondents even if phase 2 slips.

Next free ids on 2026-10-03, so the first phase in each module does not collide: Survey FR-030 /
NFR-014 / UC-012; Admin FR-038 / NFR-018 / C-018 / UC-031; Author FR-068 / NFR-022 / C-027 /
UC-048 (NFR-020 and NFR-021 are spoken for by `Author/docs/research/i18n_survey.md:650-651`).
Author's `CLAUDE.md:157-163` requires the FR row, `use_cases.puml` and the UC spec (validated
with `validate_use_case.py --strict`) before any code; Survey and Admin follow the same AIUP order.
