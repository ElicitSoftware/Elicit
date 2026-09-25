# Use Case: Install without Docker

## Overview

**Use Case ID:** UC-008  
**Use Case Name:** Install without Docker  
**Primary Actor:** Deployment Operator  
**Goal:** Install each module as a JVM service against the site's own PostgreSQL cluster and identity provider, so that Elicit runs under the site's existing service management.  
**Status:** Open  
**Requirements:** [FR-013, C-008](../requirements.md)

## Preconditions

- The database cluster is prepared (UC-005) and the identity provider is configured (UC-006).
- A Java 25 runtime and a service manager are available (UC-004).

## Main Success Scenario

1. The operator reads the module-by-module chapter, which gives one procedure repeated per module.
2. The operator obtains the module's runnable artifact for the release being installed.
3. The operator writes the module's configuration — datasource, identity, mail, brand and translations paths, and anything else the site changes — as a properties file or as environment variables, by the rules the configuration chapter gives (UC-011).
4. The operator installs the module as a service under the site's service manager, with the runtime, the working directory and the configuration source the manual specifies.
5. The operator starts the survey application first and waits for its readiness endpoint to report healthy, which is what confirms its migrations have completed.
6. The operator starts the console, whose migrations add its tables to the schema the survey application created, and waits for its readiness endpoint.
7. The operator starts the remaining modules the site runs, in any order.
8. The operator proceeds to the first sign-in (UC-009).

## Alternative Flows

### A1: The site runs the authoring tool

**Trigger:** The site authors surveys (step 7)  
**Flow:**

1. The operator points a second instance of the survey application at the authoring database and starts it, which creates the schema there, then starts the authoring tool against the same database.
2. The manual states the two settings that distinguish the preview instance: it accepts any access code so that an author can walk a draft, and it never builds or updates the reporting schema.
3. Use case continues at step 7.

### A2: A module is started out of order on a new database

**Trigger:** The console or the report service is started before the survey application has migrated (step 6)  
**Flow:**

1. The module's migration fails on an object that does not exist yet, and the module does not start.
2. The manual states that nothing is corrupted, that the fix is to let the survey application finish and start the module again, and that there is no automatic ordering here as there is in the container path.
3. Use case continues at step 5.

### A3: The site runs more than one instance of a module

**Trigger:** The site runs the survey application behind a load balancer (step 4)  
**Flow:**

1. The manual states which settings must agree across instances — the datasource, the brand and translations paths — and that the proxy-forwarding settings must be on for sign-in redirects to be built with the external address.
2. Use case continues at step 4.

### A4: Configuration is supplied as environment variables only

**Trigger:** The site's service manager provides no file-based configuration (step 3)  
**Flow:**

1. The manual gives the name-mangling rule between a setting name and its environment variable form, and notes which setting names contain characters that the rule maps ambiguously.
2. Use case continues at step 3.

## Postconditions

### Success Postconditions

- Each module the site runs is a managed service that starts on boot, reports healthy, and is configured from a source the site controls.

### Failure Postconditions

- A module fails to start and says why in its log. The database keeps whatever migrations completed; a module that failed its migration is started again after the cause is fixed.

## Business Rules

### BR-001: The first start is ordered, later starts are not

Only the first start against a new database is ordered (UC-003 BR-002). Once the schema is migrated the services may start in any order and may restart independently.

### BR-002: Readiness means migrated

A module's readiness endpoint reports healthy only after its own migrations have completed, which is what makes it the signal to wait on rather than the process having started.

### BR-003: One configuration mechanism, many sources

Every setting can be supplied as a property or as an environment variable, by the same rules, whichever the site's service manager makes easier (UC-011).

### BR-004: Upgrades are release-specific

Installing is documented here; upgrading an existing installation is not, and the manual points at the release's own procedure instead (C-006).
