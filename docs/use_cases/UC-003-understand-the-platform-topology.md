# Use Case: Understand the Platform Topology

## Overview

**Use Case ID:** UC-003  
**Use Case Name:** Understand the Platform Topology  
**Primary Actor:** Deployment Operator  
**Goal:** Learn which modules exist, what each one is for, which of them a given site needs, what they share and in what order they may first be started, so that the site can be planned before anything is installed.  
**Status:** Open  
**Requirements:** [FR-004, FR-005, FR-006](../requirements.md)

## Preconditions

- The operator has the manual (UC-001).

## Main Success Scenario

1. The operator reads the module list: the subject-facing survey application, the administration console, the authoring tool, and the two optional services — the survey-specific report service and the pedigree drawing service.
2. The system states, for each module, what it does, whether it is required, and what it needs: a database, an identity provider, a mail relay, or another module.
3. The operator reads what the modules share: one PostgreSQL cluster, one `survey` schema that the survey application owns, one identity provider, one brand directory and one translations directory.
4. The system states that the authoring tool works in a database of its own so that drafts never reach a site's survey data, and that its respondent preview is a second instance of the survey application pointed at that database.
5. The operator reads the first-start order and its reason: the survey application creates the shared `survey` schema, the console and the report service migrate on top of it, so the survey application starts and becomes healthy first.
6. The system states that the order constrains only the first start of a new database; once the schema exists the modules may be started in any order.
7. The operator reads which host ports each module listens on and which of them need to be reachable from a browser, from another module, or from neither.
8. The operator decides which modules the site will run and proceeds to the prerequisites (UC-004).

## Alternative Flows

### A1: The site runs the survey-specific report service

**Trigger:** The site will run the Family Health History Survey (step 8)  
**Flow:**

1. The manual states that the report service is specific to that one survey, that it reports not-ready until the survey definition has been installed, and that it becomes ready by itself on the next health probe afterwards.
2. The manual points at that module's own documentation for its configuration and stops (C-005).
3. Use case continues at step 8.

### A2: The site runs the authoring tool only

**Trigger:** The site authors surveys but does not deliver them (step 8)  
**Flow:**

1. The manual states that the authoring tool needs its own database, an instance of the survey application to create the schema in it, and the identity provider; it does not need the console, the report service or the pedigree service.
2. Use case continues at step 8.

### A3: The operator looks for a metrics stack

**Trigger:** The operator sees the metrics endpoints (step 7)  
**Flow:**

1. The manual states that each module exposes a metrics endpoint a site may scrape, and that this repository runs nothing that scrapes it (C-011).
2. Use case continues at step 8.

## Postconditions

### Success Postconditions

- The operator can name the modules the site will run, the resources they share, and the order of the first start.

### Failure Postconditions

- None. Reading changes nothing.

## Business Rules

### BR-001: The survey application owns the shared schema

The `survey` schema is created and migrated by the survey application. The console and the report service add their own tables to it and must never run their migrations against a database the survey application has not migrated first.

### BR-002: The order constrains the first start only

The start order exists because of schema creation. It applies to a new database. An existing, migrated database imposes no order.

### BR-003: The authoring tool is isolated by database

The authoring tool never connects to a site's survey database. Its work reaches a site only as an exported definition file that the console applies.

### BR-004: Optional means optional

A site that does not run the Family Health History Survey runs neither the report service nor the pedigree service, and the manual does not require them.
