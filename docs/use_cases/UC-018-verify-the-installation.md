# Use Case: Verify the Installation

## Overview

**Use Case ID:** UC-018  
**Use Case Name:** Verify the Installation  
**Primary Actor:** Deployment Operator  
**Goal:** Work through an ordered set of checks that confirm each part of a new installation — services reachable, applications ready, dependencies reachable, the database populated and a survey walkable — so that the site is known to work before anyone else uses it.  
**Status:** Open  
**Requirements:** [FR-032, FR-033, C-011](../requirements.md)

## Preconditions

- The site's services have been started, by either installation path (UC-007 or UC-008).
- The first sign-in is complete and a department exists, so the console can be used (UC-009).
- If the site serves a survey, its definition has been applied (UC-010).

## Main Success Scenario

1. The operator reads the verification chapter, which orders the checks from the outside in: the services are running, each application reports itself ready, each outbound dependency is reachable, the database holds what the installation should have created, and a respondent can walk a survey end to end.
2. The operator runs the status script, which lists the state of every container and then issues an unauthenticated request to each published address, reporting a service as reachable when it answers with a page, with a redirect to the identity provider or with an authentication challenge. The manual states which services the script probes and which it does not.
3. The operator reads each application's readiness and liveness endpoints under the health root path, which report whether the application's own checks pass rather than merely that a port is open. The manual states that the addresses are the same inside and outside a container but the port is not: the container health checks address the port inside the container, and the operator addresses the published one.
4. The operator reads the survey application's startup diagnostics report in the service log, which is written a short configurable delay after start and gives what is running, both database connections and the migration the schema is at, which brand resolved and where each asset came from, every outbound target it could reach, and the effective deployment settings with secrets reported only as present or absent.
5. The operator signs in to the console and works through its System section: an overview of what is running with the setup work still outstanding, the state of both database connections and each module's migration history, which brand directory resolved, the effective mail settings with a test message, a bounded reachability probe of every outbound dependency, and the current sign-in's authentication and role state.
6. The operator opens the same System section in the authoring tool, which carries every one of those screens except the mail one, and confirms that the tool's own database holds the schema its preview instance depends on.
7. The operator confirms in the database that applying the survey definition rebuilt the reporting star schema, by counting the rows of the step dimension: the count is zero on an installation that carries no survey and non-zero once one has been applied.
8. The operator completes one end-to-end walkthrough: generate an access code for a test subject in the console, enter the survey with it, answer, finalise, and generate that subject's report.
9. The operator records the outcome. Any check that did not pass is taken to the failure chapter (UC-019).

## Alternative Flows

### A1: A service answers but is not ready

**Trigger:** The status script reports a service reachable, but the site does not behave (step 2)  
**Flow:**

1. The manual states that the script's probe is a reachability check and not a health check: a redirect or an authentication challenge proves only that the application is listening.
2. The operator reads that application's readiness endpoint instead, which reports the failing check by name.
3. Use case continues at step 3.

### A2: The report service reports itself not ready

**Trigger:** The report service is not ready and refuses report requests (step 3)  
**Flow:**

1. The manual states that this is the expected state of the report service on an installation whose survey definition has not yet been applied, that it names the survey it is waiting for in one log warning, and that it answers report requests with the same instruction rather than failing silently.
2. The operator applies the survey definition (UC-010); the service becomes ready by itself on its next probe and nothing is restarted.
3. Use case continues at step 3.

### A3: A service the status script does not probe

**Trigger:** The site runs the pedigree service or the trace collector (step 2)  
**Flow:**

1. The manual states that the script probes the survey application, the console, the report service, the authoring tool and its preview instance, and the bundled identity provider and mail catcher, and that the pedigree service and the trace collector are not among them.
2. The operator checks those directly: the pedigree service through the console's connection probe, which lists every report service address the database holds, and the trace collector through the trace user interface.
3. Use case continues at step 3.

### A4: The site has no console to verify from

**Trigger:** The site runs the survey application without the console (step 5)  
**Flow:**

1. The manual states that the survey application has no administrator sign-in and therefore no System section: its equivalent is the startup diagnostics report of step 4, which covers the same ground in the log.
2. The manual states that the report is on by default, that the delay before it is written can be changed, that it can be switched off entirely, and that its findings requiring action are logged at a level that survives the log level the images run at.
3. Use case continues at step 7.

### A5: The site scrapes the metrics endpoints

**Trigger:** The site runs its own metrics collector (step 3)  
**Flow:**

1. The manual gives each application's metrics path and states that in the console and the authoring tool it is permitted without authentication, as the health paths are, and that the survey application requires no authentication for it either.
2. The manual states that this repository runs no metrics collector and that nothing here scrapes those endpoints, so the site supplies its own (BR-005).
3. Use case continues at step 4.

### A6: The reporting star schema is still a skeleton

**Trigger:** The step dimension is empty although a survey definition was applied (step 7)  
**Flow:**

1. The manual states that the migrations create only a handful of skeleton reporting tables and that the rest of the star schema is built when a definition is applied, which asks the survey application to rebuild it.
2. The operator checks that the console could reach the survey application to make that request, which the console's connection probe reports, and applies the definition again.
3. Use case continues at step 7.

## Postconditions

### Success Postconditions

- Every check has passed, and the operator has seen a survey answered and a report produced on the installed site.
- One test subject exists, with the answers the walkthrough gave.

### Failure Postconditions

- The installation is unchanged; every check in this use case is read-only except the walkthrough, which leaves the test subject it created.
- The operator holds a named failing check to take to the failure chapter (UC-019).

## Business Rules

### BR-001: Verification runs from the outside in

The checks are ordered so that each one only runs against what the previous one proved. There is no point reading an application's readiness endpoint before its container is running, or counting reporting rows before the database connection is known to work, and a failure found early explains every failure after it.

### BR-002: A reachable port is not a healthy service

Reaching a published address proves that something is listening on it. Only the readiness endpoint reports whether the application's own checks pass, and only the System screens and the startup diagnostics report say which dependency is at fault when they do not.

### BR-003: Health checks address the container

The container health checks address the port inside the container, which is the same for every application regardless of the host port it is published on (UC-004 BR-003). An operator checking from the host uses the published port instead, and a check that appears to fail is often a check aimed at the wrong one (UC-019).

### BR-004: The reporting schema is the evidence that a definition applied

An installation with no survey carries only the skeleton reporting tables the migrations create; applying a definition builds the dimension tables, the fact tables and their views. The fact tables stay empty until respondents exist and fill through the triggers on the respondent table, so an empty fact table on a site with no respondents is correct and an empty step dimension on a site with a survey is not.

### BR-005: The metrics endpoints are endpoints, not a metrics stack

Each application exposes a metrics path, but this repository runs nothing that scrapes it and serves no metrics user interface (C-011). The manual documents the endpoints as endpoints a site may point its own collector at, and describes no dashboard, query or alert that this installation provides. The trace collector the compose file does run receives traces only; its own metrics views have nothing behind them.

### BR-006: Each application reports on itself

The console and the authoring tool report their own state on their own System screens, each against its own database connection, its own brand mount and its own outbound targets. Neither reports on the other, and neither reports on the survey application, which has no sign-in and writes its report to the log instead. Verifying a site means reading all three.
