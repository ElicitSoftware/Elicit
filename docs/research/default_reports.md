# Default Reports: The Respondent Summary and the Administrator Report

> **Status (2026-10-02):** Research plan. Today a survey has **no reports unless somebody writes a
> report service for it**. Every report a respondent or an administrator sees comes from an external
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
> **Scope:** the reports are for new, generic surveys. FHHS is out of scope. It appears only as the
> existing example of a survey-specific report service (section 1.1), and nothing proposed here
> replaces or changes it.

## Overview

The plumbing for reports already exists and works. Survey's `ReportView` and Admin's
`ReportingService` both loop over `survey.reports`, POST a `ReportRequest{id, language}` to each
URL, and render the `ReportResponse{title, innerHTML, pdf}` that comes back. Both apps can then
merge the responses into one PDF. What is missing is **content**: there is no report service that
works for an arbitrary survey, and no way for an author to say which questions matter.

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
- **A home for the code.** C-009 keeps analytics dashboards out of Survey, and the reporting star
  schema still hard-codes `survey_id = 1` (section 1.5).

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
| `survey.reports(id, survey_id, name, description, url, display_order, report_key)` | Survey `V001:814`, `report_key` in `V015:25` | One row per report service, listed per survey |
| `ReportView` (`/report`) | Survey `flow/ReportView.java:56,126,173-180` | Shown after Finish, and on re-login by an inactive respondent (`MainView.java:143-158`). One `ReportCard` per report, a "Generate PDF" button, and Next → `surveys.post_survey_url` |
| `ReportingService.printReports` | Admin `service/ReportingService.java:185` | The same loop, run from the search grid for a respondent whose status is Finished |
| `PDFService`, `/api/pdf/download` | both apps | Merge each report's `PDFDocument` into one PDF |
| `survey.post_survey_actions`, `respondent_psa` | Survey `V001:834,850` | Fire-and-retry POSTs of `{"id"}` after Finish. These are not reports |

FHHS plugs in by being listed in `survey.reports`: `/pedigree/report`, `/proband/report` and
`/casummary/report` each return the contract above. They read the **star schema**, not the
answers: `CancerHistoryRepository` selects tag columns from `surveyreport.fact_sections_view` for
the respondent. That works because `finalize()` runs the per-respondent ETL before `ReportView`
calls the services, and because FHHS is survey 1, the only survey the ETL handles (G-3). **A
default report plugs in the same way**, through the contract (decided 2026-10-02, section 2.4).
Section 4.4 explains why it should not read the star schema. It then needs no new rendering, PDF
or Admin wiring, and an author can list it, reorder it or remove it alongside a custom one.

How the rows are managed today:
- **No module has a screen for them.** Nothing in Survey, Admin or Author lists, edits, reorders or
  deletes a `survey.reports` row. Rows arrive only by `.elicit` import (Admin
  `SurveyDefinitionImportService.insertReport`, `:914-930`) or by SQL. The only documented edit is
  the manual `UPDATE survey.reports SET url = …` that fixes the host after a deployment
  (`DeploymentScript.md:268-274`).
- **Update is an upsert by `report_key`.** Applying a new version of a survey matches rows by key,
  updates `name`, `description`, `url` and `display_order` in place, and inserts unknown keys
  (`SurveyDefinitionUpdateService.upsertReport`, `:1203-1236`). The file wins for every row it
  carries.
- **Update never deletes.** A row whose key is no longer in the file is left alone (class comment,
  `:73-74`); `reports` is a Type 1 table with no `effective_to`, so the retire path does not apply
  either. Removing a report in Author therefore does not remove it from a site that already has it
  (G-12).
- **Both callers order by `display_order`** (Survey `Survey.java:90-92`, Admin `Survey.java:195-197`)
  and the card heading is the row's `name`, translated by `report_key`
  (`ContentTranslator.java:285`), not the `title` the service returns.

Author does not model `reports` or `post_survey_actions` yet. Its exporter and importer pass the
rows through as raw SQL (`SurveyDefinitionExporter.java:95-96`, `SurveyDefinitionImporter.java:488-507`),
so an imported FHHS file round-trips its three rows, but an Author-built survey always exports zero
rows (`Author/docs/requirements.md:207-210`). An Author-built survey therefore cannot even *list* a
report today. This is a prerequisite for both reports.

### 1.2 The respondent lifecycle

`survey.respondents` (`V001:61-77`) carries `created_dt`, `first_access_dt`, `finalized_dt`,
`active` and a `logins` counter. There is no last-activity timestamp and no session or login
history. Status is derived the same way in three places: the `fact_respondents` trigger, ETL
`Sql.java:327-345`, and Admin's `survey.status` view.

| Status | Rule |
|---|---|
| Not Started | `first_access_dt` and `finalized_dt` both null |
| In Progress | `first_access_dt` set, `finalized_dt` null |
| Finished | `finalized_dt` set |

There are two consequences for the reports:
- **"Registered" is not a status of its own.** Every row is registered, so Registered = Not
  Started + In Progress + Finished. The funnel the administrator report needs is
  registered → started → finished.
- **There is no withdrawn or expired state.** `deactivate()` sets `finalized_dt` as well as
  `active=false` (`AccessCodeService.java:272-280`). It is only reached from Finish today, but any
  future "withdraw" built on it would be counted as Finished. Admin's `Status.java:236` javadoc
  already promises "invited/started/completed/expired", which do not exist.

There are also **two definitions of duration**: SQL uses `finalized − created`, while
`Respondent.getElapsedTime()` uses `finalized − first_access`. The report must pick one (section 7,
D-6).

### 1.3 What an answer row proves

Answer rows are created **eagerly**. `QuestionManager.init` builds a row for every unconditional
question in the survey on first entry, and again on each Next/Previous
(`QuestionManager.java:335-345, 692-711`). Rule-gated questions get a row only when a SHOW or
REPEAT rule fires (`:797-840`). From this follows:

| Fact | Meaning |
|---|---|
| Row exists, `deleted = false` | The question was **eligible** under the rules. This does not prove the page was shown |
| `text_value` not null, `saved_dt` set | Saved a value, **except**: a question with a `default_value` gets `saved_dt` at creation (`Answer.java:450-454`), so it looks answered whether or not the respondent touched it |
| `text_value` null | Unanswered, **except**: a CHECKBOX set to false is stored as NULL (`QuestionService.java:301-306`), so "no" and "no answer" are indistinguishable |
| No row | Hidden by the rules |
| `deleted = true` | Was eligible and then hidden by a changed upstream answer. These rows are **purged on Finish** (`removeDeleted`, `:2250-2256`), so no trace remains |

For a **Finished** respondent, eligible equals presented, because finishing requires passing through
every page and the review. The per-question metrics are therefore sound if they are computed over
finished respondents only. For **In Progress** respondents, the rows for pages they have not reached
already exist, so eligible overstates presented. Section 5 proposes the fix.

Section and step header rows have `question_id` null and carry the section or step name in
`display_text`. They are the natural headings for the respondent report.
`display_text_local` and `display_language` (`V019:123-124`) mean the report can be rendered in the
language the respondent answered in. Select-type answers store `select_items.coded_value`, so the
report must map the code back to the item's (translated) `display_text`.

### 1.4 Reporting tags are not report marks

Author's reporting tags are `survey.dimensions`, `ontology` and `metadata`, edited at
`survey/:id/reporting` (Author FR-023..026, UC-021..024). They attach a tag to a question, a section
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
| G-3 | `fact_respondents` triggers and ETL filter on `survey_id = 1` (`V002:127,157`, `Sql.java:277,314`) | The star schema cannot be the source for a new survey. **Fixed in Survey on 2026-10-02** (`per_survey_reporting_schema.md`): every survey has its own `report_<slug>` schema, filled by the ETL for every survey |
| G-4 | Eager answer rows | "Presented" is only sound for finished respondents |
| G-5 | Defaulted questions get `saved_dt`; false checkbox is NULL | "Answered" is wrong for both types |
| G-6 | Deleted rows purged on Finish | Cannot report "shown, then hidden by a changed answer" |
| G-7 | No last-activity timestamp | Cannot tell an active In Progress respondent from an abandoned one |
| G-8 | No withdrawn/expired state; `deactivate()` sets `finalized_dt` | A future withdraw would be counted as Finished |
| G-9 | Two definitions of duration | Admin and the star schema would disagree. **Settled 2026-10-02:** the star schema's `fact_respondents` view uses `finalized_dt - first_access_dt`, the same as `Respondent.getElapsedTime()` (Survey UC-008 BR-010); D-6 should keep to it |
| G-10 | Admin has no dashboard FR/UC, though Admin `vision.md:42-45` lists "progress monitoring dashboards" as in scope | The administrator report needs requirements before code |
| G-11 | Author calls the star-schema tags "reporting tags" and their columns "report columns", and tells authors to "tag what the report needs" | Once default reports exist, an author will expect tagging a question to put it on a report (section 2.3) |
| G-12 | Applying a survey update never removes a `reports` row whose `report_key` has left the file (`SurveyDefinitionUpdateService.java:73-74`) | An author who drops the default report, or a custom one, and republishes does not remove it from a deployed site. The row keeps running until someone deletes it by SQL (section 2.4, D-13) |

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
  page".
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
- A preview, rendered from `questions.sample`, so an author sees the summary before publishing.

### 2.3 Rename reporting tags to analysis tags

Author's tag screens describe the star schema as *the* report. Once the Reports page in section 2.2
exists, "report" would mean two unrelated things in the same tool:
- Tagging a question for the star schema, and
- marking a question for the respondent or administrator report.

The wording that causes the confusion is in `Author/src/main/resources/vaadin-i18n/translations.properties`:

| Key | Today |
|---|---|
| `reportingView.intro` (:406) | "A reporting tag names a column of **the site's report**." |
| `guideView.reportingTags.p1` (:181) | "names a column of **the report a site builds**" |
| `guideView.designing.p2` (:179) | "**tag what the report needs**". This is the most misleading: under this plan, what a report needs is a report mark |
| `reportingView.grid.reportColumn` (:412) | "Report column" |
| `surveyEditorView.reporting` (:58), `designer.menu.reportingTags` (:199), `reportingView.pageTitle` / `.heading` (:400-401), `tagBadge.title` (:296), `elementView.fact.reportingTags` (:386), `guideView.*reportingTags*` (:175-176, :180) | "Reporting…", "Reporting tags…", "Reporting tags" |

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
| The Author guide and the Author manual's chapter on tags | the `surveyreport` schema: deployed databases, grants and FHHS's queries name it |
| Author FR-023..026 and UC-021..024 titles and prose | `ReportingNames` and other class names that authors never see |
| The `survey/:id/reporting` route, renamed to `survey/:id/analysis` | the `.elicit` table names |
| Admin FR-027's title "Rebuild Reporting Schema", in Admin's docs only. The console shows no such label, and Admin's `reporting.error.*` strings are about report services, which is already the right meaning | Survey's `etl` package and `/api/etl/build` |

The rename belongs in phase 2 (section 8), before or together with the Reports page. That way the
two meanings of "report" never appear side by side in a release.

### 2.4 Default reports are rows in `survey.reports`

Decided 2026-10-02. A default report differs from FHHS's report services only in who wrote the
service. It is listed in the same table, called through the same `ReportRequest`/`ReportResponse`
contract, ordered by the same `display_order`, named by the same translated `name`, and printed by
Admin through the same loop. Nothing in `ReportView` or `ReportingService` knows that a row is a
default. The consequences:

- **Seeding.** Author's Reports page (section 2.2) pre-populates the standard row(s) when a survey
  is created. Today that is one row, the respondent report of section 3; a future standard
  per-respondent report would be a second seeded row. The rows travel in the `.elicit` like every
  other authored table and are inserted by Admin's apply.
- **The author decides.** The seeded rows are a starting point, not a fixture. An author can keep
  them, delete them, move them above or below custom rows, or add rows of their own. *Replacing*
  the default is deleting the seeded row and adding a custom one. *Supplementing* it is keeping
  both. A survey with nothing marked for the respondent report can simply drop the row, and
  finishes on the same empty page as today.
- **Author is the only editor.** The rows are authored content, so there is no Admin page for them.
  A site changes a report by republishing the survey. This keeps one source of truth, and the
  `.elicit` already carries the rows. (An Admin editing page was considered and rejected: it would
  make the rows site-local and force a merge rule on every re-apply.)
- **The file wins on re-apply.** Admin's update already upserts by `report_key` (section 1.1), so
  a republished survey restores the authored name, URL and order. This is the intended behavior,
  not a gap.
- **Removal must work.** The one missing piece is G-12: the update leaves a row alone when its key
  has gone from the file, so deleting the default report in Author does not yet delete it on a
  site. Phase 2 closes this (D-13).
- **The built-in service's URL cannot name a host.** FHHS's rows carry an absolute, site-specific
  URL (`http://host.docker.internal:8082/proband/report`) that the deployment procedure patches by
  hand. A row seeded in Author knows nothing about the site that will apply it, and the default
  service lives in Survey itself. How the row expresses that is D-12; the leaning is a relative
  path that each caller resolves against Survey's base URL, which Admin already has as
  `elicit.survey.url`.

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
| Repeat instances (e.g. one block per household member) | `section_instance` / `question_instance`, headed by the instance's own section header row |
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
- C-009 forbids *analytics dashboards* in Survey. A per-respondent echo of their own answers is not
  one.

Every new survey gets the row: Author seeds it on the Reports page, and the author deletes it if the
survey has nothing to echo back (section 2.4, D-1). If the row stays with no respondent marks, the
service renders the title, completion date and closing text only, and D-3 decides whether it says so.

### 3.4 Disclosure

The summary is reachable again by logging in with the access code (`MainView.java:143-158`), so the
summary is exactly as private as the access code. Marking a sensitive question is therefore an
authoring decision with a consequence. The Author UI should say so when marking a question in the
Respondent report. Every generation is a PHI read and must be logged (Admin NFR-005 sets the
precedent).

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
1. **A Survey Overview view** (new route, for example `survey-overview`), with a survey picker, a
   department filter, and a date range. It shows the participation tiles and funnel (4.1), the
   timeline, and one card per marked question (4.2). Admin has no charting dependency. A small set of
   charts (funnel bar, cumulative line, per-option bar) is the decision in D-7.
2. **An exportable form**: a PDF of the same content through Admin's existing `PDFService`, and a
   CSV of the aggregate tables. Aggregates only; row-level export stays the existing per-respondent
   `ELICIT_EXPORT_V2`.

Both default reports query the OLTP tables (`respondents`, `answers`, `report_items`) directly, not
the star schema, unlike FHHS (section 1.1). There are three reasons:
- **G-3:** the ETL filled the star schema only for survey 1 when this was written. Fixed on
  2026-10-02 (`per_survey_reporting_schema.md`): a new survey now gets its own schema on apply.
  The two reasons below still hold, so the default reports stay on the OLTP tables.
- **Marks are not tags.** The star schema has a column only for a *tagged* question, while a report
  mark is independent of tagging (section 1.4).
- **The star schema keeps values, not presentation.** It stores the value or tag constant per
  step/section instance. It does not keep the question text, the option labels in the respondent's
  language, rows that were presented but left unanswered, or display order, and every one of those
  is something the reports need.

Once G-3 is fixed, the star schema becomes a reasonable second source for cross-respondent
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

| Change | Module | Closes | Notes |
|---|---|---|---|
| `answers.presented_dt`: set when the page holding the row is first rendered | Survey | G-4 | Makes "presented" exact for In Progress respondents and gives drop-off and time-per-step. Pre-existing rows of finished respondents can be back-filled from `created_dt` |
| `respondents.last_activity_dt`: set on each save or navigation | Survey | G-7 | Cheap; one column |
| Distinguish a saved value from a default | Survey | G-5 | For example, set `saved_dt` only on an explicit save, and render the default without stamping |
| Store an explicit `false` for CHECKBOX | Survey | G-5 | A data-semantics change. Check ETL and FHHS readers for `IS NULL` assumptions first |
| A withdrawn/expired state that does not set `finalized_dt` | Survey + Admin | G-8 | Only needed when a withdraw feature is built. Record the constraint now |
| Keep deleted rows (or log the event) instead of purging on Finish | Survey | G-6 | Optional; storage cost versus the "answer changed" metric |
| Model `reports` and `report_items` in Author and the `.elicit` format; the Reports page seeds the default row into every new survey | Author, Survey (migration), Admin (apply) | G-1, G-2 | Format stays `ELICIT_SURVEY_EXPORT_V1` if the importer tolerates an unknown trailing table. Verify that before choosing |
| Remove a `reports` row on update when its `report_key` has left the file | Admin | G-12 | Delete or retire per D-13. Today's update never deletes (`SurveyDefinitionUpdateService.java:73-74`) |
| Resolve a relative report URL against Survey's base URL in both callers | Survey, Admin | — | Needed by D-12 if the relative-path option is chosen; Admin has `elicit.survey.url` already, Survey needs its own base URL |

## 6. Research questions

These need an answer from the code or a prototype before requirements are written:

1. **Does `presented_dt` belong on the row or the page?** Rows are rebuilt on each Next/Previous
   (`QuestionManager.init`). Find the one place a page is rendered for the first time and confirm it
   can stamp only its own rows.
2. **Default values.** Which existing surveys rely on `saved_dt` being set for defaulted questions
   (review, validation, the ETL `saved_dt IS NOT NULL` filter at `Sql.java:282-325`)?
3. **Checkbox false.** Which readers (ETL, FHHS, Admin export) treat NULL as false? Changing the
   storage is a migration plus a reader audit.
4. **Repeats.** For a question inside a REPEAT section, is "presented" counted per respondent or per
   instance? Prototype both on a household-style survey and see which one a coordinator reads
   correctly.
5. **Query cost.** Measure the per-question aggregate over a synthetic 10 000-respondent survey on
   the dev database before deciding between live queries and a materialized summary.
6. **Charts in Admin.** Vaadin Charts is commercial. Weigh it against a lightweight SVG renderer for
   three chart types, rendered server-side so the PDF and the screen match.
7. **The `.elicit` format.** Does the V1 importer skip an unknown table, or does `report_items` need
   a format version bump?
8. **Translations.** Confirm `survey.translations` can carry a new `element_type` for report labels
   and the closing text without a Survey migration beyond the check constraint.

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
| D-11 | User-facing name for star-schema tags | keep "reporting tags" / "analysis tags" / "data tags" | "analysis tags", changing only user-facing text (section 2.3) |
| D-12 | How the seeded row names the built-in service | absolute URL patched per site after apply (as FHHS, `DeploymentScript.md:268-274`) / a relative path such as `/api/reports/respondent-summary` that `ReportView` and `ReportingService` resolve against Survey's base URL / a `builtin:` scheme dispatched in-process | relative path: Admin has `elicit.survey.url`, Survey needs one base-URL setting, and custom rows keep working unchanged |
| D-13 | A `reports` row whose key has left the republished file | delete / retire with an `effective_to` / leave as today | delete: `reports` is a Type 1 table with no history, and leaving it running is G-12 |

## 8. Plan

Under AIUP, docs come before code in every module. Each phase starts with FR + `use_cases.puml` + UC
spec in the module it touches.

| Phase | Work | Modules | Verification |
|---|---|---|---|
| 0 | Answer the research questions in section 6; settle the D-table | — | this document updated |
| 1 | Data capture: `presented_dt`, `last_activity_dt`, default/checkbox semantics (section 5) | Survey | greenfield and brownfield (`resetDatabase.sh V2`) runs, ETL output unchanged for survey 1 |
| 2 | Rename reporting tags to analysis tags in Author's UI, guide, manual and docs (section 2.3); model `survey.reports` and `report_items` in Author, with the Reports page seeding the default row (section 2.4); `.elicit` round-trip; Admin apply deletes unlisted `reports` rows (D-13); report URL resolution (D-12) | Author, Survey (migration), Admin | export, apply and re-export give identical files; a survey republished without the default row no longer lists it after apply; Author's three-way build check |
| 3 | Respondent report service in Survey; Author preview | Survey, Author | a new test survey with marks in every question type, in en, es-419 and ar (RTL); PDF and card match |
| 4 | Administrator report v1: participation, marked questions, tables, PDF/CSV, suppression | Admin | counts reconcile with direct SQL on a seeded survey; department scoping; k-suppression |
| 5 | v2: charts, drop-off by step, stalled respondents, time per step | Admin | the same seeded survey with In Progress respondents at known steps |

The phases are ordered so that each one is useful on its own:
- Phase 2 alone lets an author list a custom report.
- Phase 3 alone gives every respondent a summary.
- Phase 4 runs on finished respondents even if phase 1 slips.
