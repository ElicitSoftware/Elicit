# Use Case: Install the Family History Reports

## Overview

**Use Case ID:** UC-021  
**Use Case Name:** Install the Family History Reports  
**Primary Actor:** Deployment Operator  
**Goal:** Run FHHS and Pedigree, the report services of the Family Health History Survey, beside a site's Survey and Admin, so that a respondent who finishes that survey sees their summary, their relatives' cancer history and their pedigree drawing, and so that a report developer has a working report service to read (UC-020).  
**Status:** Draft  
**Requirements:** [FR-042, FR-038, FR-041](../requirements.md)

## Preconditions

- Survey and Admin are installed and ready (UC-007 or UC-008), and the first sign-in is complete (UC-009).
- The operator has the FHHS and Pedigree images at the same release tag as Survey and Admin.
- The operator knows the address at which Survey and Admin will reach FHHS.

## Main Success Scenario

1. The operator reads the Family History reports chapter, which states that FHHS answers the three reports of the Family Health History Survey and the post-survey action that files the family history elsewhere, and that Pedigree draws the family tree FHHS asks it for.
2. The system states that both are optional and specific to that one survey: a site that does not run the Family Health History Survey runs neither.
3. The operator starts Pedigree. It needs no configuration and no database.
4. The operator configures FHHS with its database connections, the address of Pedigree, and whether it files reports elsewhere, using the settings the chapter lists.
5. The operator starts FHHS after Admin is ready, as the compose file does.
6. The system shows FHHS reporting not-ready, with a reason naming the survey it is waiting for, because no Family Health History Survey has been installed yet.
7. The operator makes sure the report addresses in the survey's definition name FHHS at the address Survey and Admin reach it at, changing them in Author and exporting again if the site's address differs from the shipped one.
8. The operator applies the Family Health History Survey definition in the console (UC-010).
9. The system shows FHHS becoming ready on its next readiness probe, with nothing restarted.
10. The operator opens Connections in the console's System section and sees each of the survey's report services answer.
11. The operator walks the survey as a respondent to the end and sees the three reports, the pedigree drawing among them.
12. The site runs the Family History reports, and the report developer has a running example of the report contract to read.

## Alternative Flows

### A1: The site files the family history in a records system

**Trigger:** The site wants each finished family history delivered as a document (step 4)  
**Flow:**

1. The manual states that FHHS answers the survey's post-survey action by building the PDF of all the survey's reports and uploading it, with an index document, to an SFTP server.
2. The operator turns the upload on and gives the server's host, port, path and account, with either a password or a private key, and the server's known host key.
3. The manual states that FHHS records each upload against the respondent and retries failed ones every fifteen minutes, up to fifty times.
4. Use case continues at step 5.

### A2: FHHS stays not-ready after the apply

**Trigger:** FHHS still reports not-ready after the definition is applied (step 9)  
**Flow:**

1. The manual states the two causes: the survey installed is not the one FHHS is configured for, or its reporting schema has not been built yet.
2. The system names which of the two in FHHS's readiness reason and in the warning it logs.
3. The operator applies the right definition, or applies it again to build the reporting schema.
4. Use case continues at step 9.

### A3: The pedigree report shows an error

**Trigger:** The pedigree card shows an error in place of the drawing (step 11)  
**Flow:**

1. The manual states that FHHS waits a short, fixed time for Pedigree and shows Pedigree's error, or the failure to reach it, in the card.
2. The operator checks that Pedigree is running and that FHHS's Pedigree address reaches it from inside the FHHS container.
3. Use case continues at step 11.

### A4: Report requests are refused before the survey is installed

**Trigger:** A report is requested while FHHS is not ready (step 6)  
**Flow:**

1. The manual states that FHHS refuses every report request until it is ready, answering with the same reason its readiness check gives.
2. Use case continues at step 7.

## Postconditions

### Success Postconditions

- FHHS and Pedigree run at the same release as Survey and Admin, FHHS reports ready, and the survey's report services answer on the Connections screen.
- A respondent who finishes the Family Health History Survey sees their three reports.

### Failure Postconditions

- Survey and Admin are unaffected. The survey's reports show an error card, and FHHS reports not-ready with a reason naming what is missing.

## Business Rules

### BR-001: FHHS serves one survey, by key

FHHS is configured with the key of the survey it serves (`family.history.survey.key`). Until a survey with that key is installed and its reporting schema built, FHHS reports not-ready and refuses report requests with the reason (FHHS UC-005).

### BR-002: The schema name is resolved per request

FHHS reads the survey's reporting schema name on every request, so it follows a rename and needs no restart after an apply (UC-020 BR-005).

### BR-003: FHHS starts after Admin

On a new database FHHS's migrations and Admin's grant on the same tables. Started together they can collide, so FHHS starts once Admin is ready; Admin needs nothing from FHHS.

### BR-004: The shipped report addresses are an evaluation's

The definition file ships with report addresses that reach FHHS on the evaluation host. A site whose Survey and Admin reach FHHS at another address changes them in Author and exports again; the report list is replaced on every apply (UC-020 BR-006).

### BR-005: Pedigree takes no configuration

Pedigree reads no settings and holds no data. It draws one pedigree per request from the table FHHS sends it and keeps nothing.

### BR-006: Report endpoints carry no authentication

Survey and Admin call report services without credentials, and FHHS does not check any. FHHS and Pedigree are therefore reached only on the network the applications share, and their ports are not published outside it on a real site.
