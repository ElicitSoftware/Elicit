# Use Case: Install a Survey Definition

## Overview

**Use Case ID:** UC-010  
**Use Case Name:** Install a Survey Definition  
**Primary Actor:** Deployment Operator  
**Secondary Actor:** Survey Administrator  
**Goal:** Give a new installation the survey it will run, by applying a definition file through the console, so that the operator neither looks for a seeded survey nor restarts anything to make it reportable.  
**Status:** Open  
**Requirements:** [FR-015](../requirements.md)

## Preconditions

- The first sign-in is complete: a department exists and the console is usable (UC-009).
- The operator, or an administrator acting for them, holds the administrator role the apply screen requires (UC-006).
- The operator has the definition file for the survey this site is to run.

## Main Success Scenario

1. The operator reads the survey chapter, which states that no installation path seeds a survey and that a survey reaches a site only as a definition file applied through the console.
2. The system, while no survey is installed, carries a warning on every console screen and, on each screen whose content depends on a survey, explains the empty result area and offers the screen where a definition is applied (Admin UC-019).
3. The operator obtains the definition file for the survey the site runs — an exported file carrying one survey, its reports and its post-survey actions, distributed unchanged to every site of a rollout. The repository ships the Family Health History Survey's file alongside the module that reports on it.
4. The operator opens Apply Survey Definition and uploads the file, without nominating a target survey (Admin UC-018).
5. The system reads the file's stable survey key, finds no survey installed under it, installs the definition as a new survey and reports how many records of each kind it wrote (Admin UC-014).
6. The system then asks the survey application to rebuild its reporting schema, which grows from the six tables the migrations created to the full set of dimensions, fact tables and views for the survey just installed, and adds to the result whether the rebuild happened. The survey application is not restarted (Admin UC-018 BR-107).
7. The operator confirms the outcome: the console's missing-survey warning is gone on the next screen, the apply result reports the rebuild, and the reporting schema's step dimension is no longer empty.
8. The operator confirms, where the site runs the report service, that it has become ready by itself on its next readiness probe, with nothing restarted (FHHS UC-005).
9. The operator proceeds to verify the installation as a whole (UC-018), and hands the site over to its administrators and authors, whose own manuals take it from here.

## Alternative Flows

### A1: The survey is already installed at this site

**Trigger:** A survey with the file's stable key already exists, because the site is being brought to a newer revision (step 5)  
**Flow:**

1. The system applies the file to the installed survey in place rather than creating a second one, keeping the site's own respondents, subjects and reports attached to it, and refuses a file whose revision is older than the one already applied (Admin UC-017).
2. Use case continues at step 6.

### A2: The reporting schema is not rebuilt

**Trigger:** The survey application cannot be reached, does not answer in time, or reports that the rebuild failed (step 6)  
**Flow:**

1. The system reports the apply as successful and gives the reason the rebuild did not happen as the last line of the result, and logs it. The definition is installed either way.
2. The manual states the two remedies — apply the same file again once the cause is fixed, which reconciles every record as unchanged, or restart the survey application, whose own start-up build covers the same ground — and names the failure that is actually seen, two surveys at one site whose steps or sections carry the same dimension name (Admin UC-018 A5).
3. Use case continues at step 7.

### A3: The site rebuilds the reporting schema on a restart instead

**Trigger:** The site has switched the rebuild call off deliberately (step 6)  
**Flow:**

1. The system asks the survey application nothing and the result carries no reporting line.
2. The manual states that the reporting schema is then built at the survey application's next start, and that the setting exists for a site that restarts it after every apply or runs it without reporting at all.
3. Use case continues at step 7.

### A4: The file is refused

**Trigger:** The upload carries no survey record, or a survey record with no stable key (step 5)  
**Flow:**

1. The system refuses the file and installs nothing, rather than guessing which survey it belongs to, because a duplicate survey is harder to undo than a rejected upload (Admin UC-018 BR-075).
2. The operator obtains a file exported by a current authoring tool and repeats the upload.
3. Use case continues at step 4.

### A5: The report service stays not-ready after the apply

**Trigger:** The report service still reports not-ready and refuses reports once a survey is installed (step 8)  
**Flow:**

1. The manual states that the report service serves one named survey and recognises it by key, so a site that installed a different survey, or configured the service with the wrong key, is in the same position as one that installed nothing (FHHS UC-005 A1).
2. The operator installs the survey that service serves, or corrects the key it is configured with (UC-011).
3. Use case continues at step 8.

## Postconditions

### Success Postconditions

- The site carries the survey under the stable key its definition file names, with that survey's reports and post-survey actions.
- The reporting schema carries the dimensions, fact tables and views for that survey, built without restarting the survey application.
- The console no longer warns that a survey is missing, and the report service, where the site runs one, is ready.

### Failure Postconditions

- Nothing is installed: a refused or failed apply is rolled back whole, so the site is exactly as it was and the file can be applied again once its cause is fixed (Admin UC-014 BR-049).

## Business Rules

### BR-001: No installation path seeds a survey

Neither installation path creates a survey, and no migration does either. The definition file carries the survey, its report definitions and its post-survey actions, so the only way a site acquires a survey is by applying one (FHHS UC-005 BR-005).

### BR-002: The same file installs and updates

Whether an apply is an installation or an update is a property of the receiving site, not of the file: the console looks the file's stable survey key up locally and decides. One file is therefore distributed unchanged to every site of a rollout, and no per-site instruction accompanies it (Admin UC-018 BR-073).

### BR-003: The apply rebuilds the reporting schema, a restart is not the remedy

The survey application builds its reporting schema from the surveys present when it starts, so a survey applied afterwards would have no dimensions until the next start. The console therefore asks it to rebuild once the apply has committed. The rebuild's outcome is reported and never fails the apply, because a reporting schema that is behind can be caught up and a rolled-back apply cannot (Admin UC-018 BR-107, BR-108).

### BR-004: Empty fact tables are not a fault

Installing a definition fills the reporting schema's dimensions and nothing else. The fact tables gain rows only as respondents are registered and progress through the survey, written by triggers on the respondent table. A newly installed site whose dimensions are populated and whose fact tables are empty is correct.

### BR-005: The report service is specific to one survey

The report service serves the Family Health History Survey alone and holds the key of the survey it serves. Until that survey is installed it logs the instruction once, reports not-ready and refuses report requests with the same message; it becomes ready on its next probe after the apply, with nothing restarted. The console depends on that service having started rather than being healthy, so the apply is always reachable (UC-007 BR-003, FHHS UC-005).

### BR-006: Installing a survey issues no access codes

A definition file describes a survey; it carries no subjects, no respondents and no access codes. A respondent reaches the installed survey with an access code the console issues once a subject has been registered against a department, which is the administrator's manual's subject and not this one's (C-010, Admin UC-029).
