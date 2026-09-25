# Use Case: Diagnose a Failed Installation

## Overview

**Use Case ID:** UC-019  
**Use Case Name:** Diagnose a Failed Installation  
**Primary Actor:** Deployment Operator  
**Goal:** Match a failing installation against the failures that have actually been seen, each with its cause and its fix, so that a stalled install has a documented next step rather than a search through five repositories.  
**Status:** Open  
**Requirements:** [FR-034, FR-035, C-006](../requirements.md)

## Preconditions

- An installation has been attempted and a check did not pass (UC-018), or a service never became healthy (UC-007 A2).
- The operator can read the services' logs and connect to the database.

## Main Success Scenario

1. The operator arrives at the failure chapter from the check that did not pass, each verification step naming the failures it can surface.
2. The system gives the failure table: the symptom as the operator meets it, the cause, and the fix. Every row is a failure that has been observed on a real installation rather than one imagined for completeness.
3. The operator matches the symptom. The table covers a module whose migration failed on a missing object because it was started before the module that creates it; the report service refusing every report request; a sign-in that leaves the browser at the identity provider or returns to an error; a database that refuses to start on the ownership of its data directory; and a health check that appears to fail although the application is well.
4. The operator applies the fix the row gives and re-runs only the check that failed.
5. Where the symptom is a migration that failed on a missing object, the system states that nothing is corrupted and that the module needs only to be started again once the module it depends on has finished — a failed migration leaves the schema at the version before it, and the module re-applies from there.
6. Where the symptom is the report service refusing requests, the system states that this resolves itself once the survey definition is applied, on that service's next probe and with no restart (UC-018 A2).
7. Where no row matches, the system gives the ordered sources to read next and what each one answers: the failing service's log for the exception, the survey application's startup diagnostics report for what it could and could not reach at start, the console's System section for the state of a running deployment, and the database's migration history for which module reached which version.
8. The operator repeats the verification from the first check the failure invalidated (UC-018).

## Alternative Flows

### A1: The failure is on a deployment being upgraded

**Trigger:** The installation is an existing site moving to a new release rather than a new one (step 3)  
**Flow:**

1. The system states that release-specific upgrade procedures are not in this manual, because a stamped manual outlives the release it was built for (C-006).
2. The system points at the deployment script document in this repository, which carries the upgrade procedures — the ordering the modules must be upgraded in, which migrations depend on which, and the configuration and template changes each upgrade requires — and names the sections rather than restating them.
3. The operator follows the procedure there and returns.
4. Use case continues at step 8.

### A2: The database refuses to start on its data directory's ownership

**Trigger:** The database's first start after a reset reports wrong ownership of its data directory (step 3)  
**Flow:**

1. The system states that this is a known file-sharing fault of one container runtime on one host operating system, not a broken data directory: the bind mount reports every file as owned by the superuser inside the container and the database's own ownership check intermittently fails on initial start.
2. The system gives the two fixes: run the reset again and start the stack again, or change the runtime's file-sharing implementation to the alternative one.
3. Use case continues at step 4.

### A3: The sign-in redirect does not complete

**Trigger:** Signing in leaves the browser at the identity provider, or returns to the application with an error (step 3)  
**Flow:**

1. The system states the cause: the application and the browser must both reach the provider at the address the provider issues as its own, because the browser is redirected to it and the application validates against it. An address that resolves to the provider from inside the container but to nothing from the browser, or the reverse, fails here and nowhere earlier.
2. The system gives the fix: publish one address for the provider that resolves to it from both positions, and set the application's provider address and the client's redirect address to match it (UC-006).
3. Use case continues at step 4.

### A4: A health check fails although the application is well

**Trigger:** A health check reports a service unhealthy while the application answers normally in a browser (step 3)  
**Flow:**

1. The system states the usual cause: the check was aimed at the port the service is published on rather than the port it listens on inside its container. The published ports differ per application; the internal one does not (UC-018 BR-003).
2. The system gives the fix: address the internal port from a check that runs inside the container, and the published port from a check that runs on the host.
3. Use case continues at step 4.

### A5: The installation has to be started again from an empty database

**Trigger:** The fix is not worth applying to a database an evaluation created, or the schema is in a state no row of the table describes (step 4)  
**Flow:**

1. The system states that the repository's reset script has two modes: one that empties the data directory for a new installation, and one that replaces it with a copy of an earlier-version database so that an upgrade can be exercised.
2. The system states that the reset destroys every respondent and every applied definition in that database and is for an evaluation site only (BR-005).
3. The operator resets and installs again (UC-007).
4. Use case continues at step 8.

### A6: The failure is in a site dependency rather than in Elicit

**Trigger:** The database, the identity provider or the mail relay is the site's own and is itself at fault (step 7)  
**Flow:**

1. The system states which of the console's System screens names that dependency and what it reports about it, so that the fault can be handed to whoever runs it with the probe's own result attached.
2. The system states that this manual stops at the boundary: what Elicit expects of each dependency is documented, how the site runs it is not.
3. Use case continues at step 8.

## Postconditions

### Success Postconditions

- The failure is identified, its fix has been applied, and the verification that surfaced it now passes.

### Failure Postconditions

- The symptom matched no row and the ordered sources did not identify it. The operator holds a log, a diagnostics report and a migration history, which is what a report of the failure needs to carry.

## Business Rules

### BR-001: Upgrade procedures stay out of the manual

Release-specific upgrade procedures live in the deployment script document and the manual points at them (C-006, FR-035). A stamped manual outlives the release it describes, and an upgrade procedure followed from a manual older than the release being upgraded to is worse than no procedure.

### BR-002: The table records failures that have happened

Every row names a failure observed on a real installation, with the symptom worded as the operator meets it rather than as the code raises it. A failure that has never been seen is not listed, because a table padded with speculation is one an operator stops reading.

### BR-003: A failed migration is recoverable by starting the module again

No module's migration leaves the schema half-applied: a migration that fails leaves the schema at the version before it, and the module applies from there when it is started again. A module that failed because it was started before the module that creates what it builds on needs no repair, no manual statement and no reset — only the right order and a second start.

### BR-004: No restart sequence is part of a healthy install

Bringing the site up once is enough, and applying a survey definition rebuilds the reporting schema and readies the report service without one (UC-007 BR-001). A procedure that calls for restarting a service in a fixed order is a symptom to diagnose here, not a step to adopt.

### BR-005: A reset is for an evaluation site only

The reset script deletes or replaces the database's data directory outright. It is offered as a way back to a known state on a site that carries nothing worth keeping, and the manual says so wherever it is mentioned, because the same command on a site with respondents destroys their answers with no warning and no rollback.

### BR-006: Diagnose from the running deployment, not only from the logs

Where a deployment is running, its own System screens are the first source: they report both database connections, each module's migration history, the resolved brand, the effective mail settings and a bounded probe of every outbound dependency, against the deployment as configured rather than as intended (FR-033). The logs answer what happened at start; the screens answer what is true now.
