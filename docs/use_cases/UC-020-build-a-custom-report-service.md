# Use Case: Build a Custom Report Service

## Overview

**Use Case ID:** UC-020  
**Use Case Name:** Build a Custom Report Service  
**Primary Actor:** Report Developer  
**Goal:** Give a survey a report of the site's own — shown to the respondent when they finish, and printed by the console for an administrator — by writing a small web service that reads the survey's reporting schema for one respondent and answers with the report, and by registering that service with the survey.  
**Status:** Draft  
**Requirements:** [FR-038, FR-039, FR-040, FR-041, C-017](../requirements.md)

## Preconditions

- A site is installed and the survey the report is for has been applied, so its reporting schema exists and is named on the survey (UC-010).
- The developer can run a web service on a private network, at an address the Survey and Admin applications can reach and the Internet cannot, and can give it a database connection to the site's survey database.
- The developer, or a survey author working with them, can edit the survey in Author and export it.

## Main Success Scenario

1. The developer reads the custom reports chapter, which states what a report is: one entry in the survey's ordered list of report services, each a name, a description and an address.
2. The system states when a report is asked for: by Survey, on the report page a respondent reaches when they finish, once per entry in list order; and by the console, when an administrator prints the reports of a finished respondent.
3. The system gives the request each service receives — the respondent's id and the language the respondent answered in — and the time each caller waits for an answer.
4. The system gives the answer a service returns: a title, an HTML fragment that Survey places in the report page, and a description of the same report as printable content that the console and Survey's PDF download set in type.
5. The developer connects the service to the site's survey database with a role that may read the reporting schemas.
6. The developer has the service look up the survey's reporting schema by the survey's key on every request, because the schema name belongs to the site and can be renamed while the service runs.
7. The developer reads one respondent's answers from the schema's section view, filtered on the respondent's id, and their progress and dates from the respondent view.
8. The developer writes the HTML fragment and the printable description from those rows and returns them.
9. The developer, or a survey author, adds the service to the survey on the Reports page in Author, giving its name, description and address, places it in the list, and exports the survey.
10. The administrator applies the exported definition at the site (UC-010), which replaces the survey's report list with the one in the file.
11. The administrator opens Connections in the console's System section, which reports whether the new service answers.
12. A respondent finishes the survey and sees the report on their report page, and an administrator prints it from the respondent's row in the console.
13. The survey has a custom report that the site wrote, delivered through the definition file and drawn from the reporting schema.

## Alternative Flows

### A1: The service also delivers the report elsewhere when a respondent finishes

**Trigger:** The site needs the report filed in another system, not only shown (step 9)  
**Flow:**

1. The manual states that a survey also carries an ordered list of post-survey actions, each called by Survey at the moment the respondent finishes, with the method, address and identifiers its author chose and the credential the site configured for it.
2. The manual states that an action is independent of the report list, that a failed call is recorded against the respondent, retried by Survey on a schedule up to a limit, and never stops the respondent finishing, and that once a call is answered, what the service does next (such as filing a document) is the service's to record and retry.
3. The manual names the Family History report service's upload to a records system as the worked example (UC-021).
4. Use case continues at step 9.

### A2: The service is not reachable from the console

**Trigger:** Connections reports the service as unreachable (step 11)  
**Flow:**

1. The manual states that the address is called from inside the Survey and Admin containers, so an address that works from the developer's browser may not resolve there.
2. The manual states that changing the address is an edit in Author and a new export, because a hand edit in the database is undone by the next apply.
3. Use case continues at step 9.

### A3: The service answers with an error, or too slowly

**Trigger:** The respondent's report page shows an error card in place of the report (step 12)  
**Flow:**

1. The manual states that Survey shows the error the service returned, or that it timed out, in that report's place, and still shows the other reports.
2. The manual states that the console prints an error block for that report and still prints the others.
3. The developer corrects the service; nothing at the site needs reapplying.
4. Use case continues at step 12.

### A4: The report shows on the page but not in the console's printout

**Trigger:** An administrator's printed report lacks the service's report (step 12)  
**Flow:**

1. The manual states that the console prints only the printable description, never the HTML fragment, so a service that returns no printable description is left out of the printout.
2. The developer adds the printable description to the answer.
3. Use case continues at step 12.

### A5: The survey's reporting schema was renamed

**Trigger:** An administrator renames the survey's reporting schema while the service runs (step 7)  
**Flow:**

1. The manual states that a service which looks the schema up on every request, as step 6 requires, follows the rename on its next request with nothing restarted.
2. Use case continues at step 7.

## Postconditions

### Success Postconditions

- The survey's report list names the service, in the position the author chose, and the list arrived at the site in the definition file.
- A finished respondent's report page shows the service's report, and the console's printout includes it.
- The service reads only the reporting schema and resolves its name on every request.

### Failure Postconditions

- The survey's other reports are shown and printed as before; a failing or unreachable service affects only its own report.
- The respondent finishes the survey whatever the report services do.

## Business Rules

### BR-001: A report is an address in the survey's list

A survey's reports are the entries of its report list, in display order. Each entry is a name and description, which a survey author may translate, and an address. An address beginning with a single `/` names a report built into Survey and is answered inside Survey; any other address is a full `http(s)` address of a service the site runs.

### BR-002: The request carries the respondent and the language

Each report service is sent a JSON `POST` to its address carrying `id`, the respondent's id (`survey.respondents.id`), and `language`, the tag of the language the respondent answered in, or nothing for the survey's base language. Survey waits 5 seconds to connect and 60 seconds for the answer; the console uses the REST client's own defaults. No credential is sent.

### BR-003: The answer carries three things

A service answers with JSON carrying `title`, `innerHTML` (an HTML fragment Survey places as it is in the report card, which therefore must come from the site's own trusted service) and `pdf`, a printable description: a title, an orientation, whether it starts a page, and a list of content blocks, each a text, an SVG drawing or a table. Survey heads each card with the report's translated name from the list, not with the returned title.

### BR-004: The console prints only the printable description

The console's printout is assembled from each answer's `pdf`. An answer with no `pdf` is omitted from the printout, though Survey's report page still shows its HTML.

### BR-005: Read the reporting schema, resolved per request

A report service reads the survey's own reporting schema, whose name is on `survey.surveys.report_schema` for the survey's key. The name is chosen by the site and can be renamed at any time (UC-010), so a service resolves it on every request and never hard-codes it. One respondent's answers are the rows of `<schema>.fact_sections_view` whose `respondent_id` is the requested id. The reporting role, `surveyreport_user`, may read every reporting schema and, column by column, `survey.surveys.survey_key` and `report_schema`, which Survey grants (Survey UC-008 BR-014); a service connecting as a role of its own is granted the same columns by the site.

### BR-006: Report services are registered in the definition

The report list is carried in the definition file and applied as a whole: an apply replaces the site's list with the file's, removing any entry the file no longer carries. The list is therefore edited in Author, never in the site's database.

### BR-007: Post-survey actions are called as configured, and a failed call is retried

When a respondent finishes, Survey calls each post-survey action in execution order (Survey UC-015). The call uses the action's method (`POST`, `GET` or `PUT`); its address may carry `{respondentId}` and, when the author turned on sending the subject's External ID, `{externalId}`; a `POST` or `PUT` body is `{"id": <respondent id>}`, with `externalId` and `departmentCode` added for such an action, which must use an `https` address. A site gives an action a credential in Survey's configuration, by the action's key: a bearer token the service issued, which Survey presents as given and never obtains or refreshes; nothing secret is in the definition file. Survey waits 5 seconds to connect and 60 for the answer, and any 2xx is delivered. A failed call is recorded and retried on a schedule (`elicit.post-survey-action.retry.*`) until it is delivered or the limit is reached; a call refused before it is made is not retried; a status the service writes itself is never overwritten or retried by Survey. The respondent's finishing is never affected.

### BR-008: Reachability is checked, never generation

The console's Connections screen sends each report service and post-survey action one read-only request and reports whether anything answered. It never asks for a report and never fires an action, so it is safe at any time.

### BR-009: A report service is headless and reachable only by its callers

A report service has no user interface: no pages, no sign-in, nothing a person opens in a browser. A respondent's browser never calls it; Survey calls it on the respondent's behalf, and Admin on the administrator's. The service therefore runs without exposure to the Internet, on a network or behind a firewall rule that admits only the Survey and Admin hosts (and, for a service another report service calls, as FHHS calls Pedigree, that service). That isolation is why no authentication is needed: neither caller presents a credential and the service checks none (C-017). This rule is for report services; post-survey actions are not bound by it.
