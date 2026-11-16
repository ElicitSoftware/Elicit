# Use Case: Evaluate with Docker Compose

## Overview

**Use Case ID:** UC-007  
**Use Case Name:** Evaluate with Docker Compose  
**Primary Actor:** Deployment Operator  
**Goal:** Bring up a complete Elicit site with one compose file, so that the platform can be developed against and evaluated without assembling each module by hand, knowing that the result is not a production installation.  
**Status:** Open  
**Requirements:** [FR-012, C-018](../requirements.md)

## Preconditions

- A container runtime and compose are installed (UC-004).
- The operator has the umbrella repository checked out, which carries the compose file, the default brand and the default translations.

## Main Success Scenario

1. The operator reads the compose chapter, which opens by stating that the stack is for development and evaluation and not for a site that holds real respondents' answers (BR-004), and names every service the file defines, the host port each publishes, the volumes each mounts and what each depends on.
2. The system distinguishes the application services from the supporting services the file bundles for evaluation — the database, the identity provider, the mail catcher, the file-transfer server and the trace collector.
3. The operator changes only what the evaluation needs: the brand directory (UC-014), the languages offered (UC-015), the services not being evaluated (A4) and any setting from the configuration reference they want to try (UC-011). The documented passwords and the permissive defaults stay, because they are what makes the stack start in one command (BR-006).
4. The operator chooses the release to run: the compose file pulls every application image at one tag, taken from one variable, so that the site runs a matched set (BR-005). Left unset, the tag is the one the local build produces; a published release is run by naming its version.
5. The operator brings the stack up in one command.
6. The system starts the database, waits for it to become healthy, then starts the survey application, which creates and migrates the shared schema.
7. The system starts the remaining services as their dependencies become healthy: the console, and the two optional services if the site runs them.
8. The operator watches the services become healthy and reads the status script's output.
9. The operator proceeds to the first sign-in (UC-009).
10. The operator who goes on to install a real site reads what carries over from the evaluation and what does not (A1).

## Alternative Flows

### A1: The operator wants a real site

**Trigger:** The evaluation is over and a site that will hold real respondents' answers is wanted (step 10), or the operator came to this chapter looking for a production installation (step 1)  
**Flow:**

1. The manual states what carries over: the brand directory, the translations directory, the definition files, and the settings the evaluation showed the site needs.
2. The manual states what does not: the compose file, every bundled service, every documented password and the evaluation database's contents.
3. The manual sends the operator to the database (UC-005), the identity provider (UC-006), the installation of a real site (UC-008) and the security chapter (UC-022).
4. Use case ends.

### A2: A service never becomes healthy

**Trigger:** A service stays unhealthy past its start period (step 7)  
**Flow:**

1. The operator reads the failure table (UC-019), which names the failures that are actually seen with their cause and fix.
2. Use case continues at step 7.

### A3: The database's first start fails on data directory ownership

**Trigger:** The database reports wrong ownership of its data directory on the first start after a reset (step 5)  
**Flow:**

1. The manual states that this is a known file-sharing fault of one container runtime on one host operating system rather than a broken copy, and gives the two fixes: repeat the reset and start again, or change the runtime's file sharing implementation.
2. Use case continues at step 4.

### A4: The evaluation does not include the report service

**Trigger:** The operator is not evaluating the Family Health History Survey (step 3)  
**Flow:**

1. The operator removes the report service and the pedigree service from the compose file, and the console's dependency on them.
2. Use case continues at step 4.

### A5: A published port is taken

**Trigger:** A host port the file publishes is already in use (step 4)  
**Flow:**

1. The operator changes the published port, and the identity provider's redirect URI for that application to match (UC-006).
2. Use case continues at step 4.

## Postconditions

### Success Postconditions

- Every service the site runs is healthy, the shared schema exists and is migrated, and the console is reachable for the first sign-in.

### Failure Postconditions

- Some services run and some do not. The database keeps whatever the migrations completed; a reset script returns the stack to an empty database for a clean attempt.

## Business Rules

### BR-001: No restart sequence is required

Bringing the stack up once is enough. The survey application creates the schema, the console applies a survey definition later, and the report service becomes ready by itself on its next probe. Older instructions describing a repeated restart no longer apply.

### BR-002: Health checks address the container

Each health check targets the port inside the container, which is the same for every application service regardless of the host port it is published on (UC-004 BR-003).

### BR-003: The console does not wait for the report service to be healthy

The console depends on the report service having started, not on it being healthy, so that the console comes up on a new database and the survey definition can be applied through it (UC-010).

### BR-004: The compose stack is for development and evaluation

The compose file and the services bundled with it are a working example of a site, for developers, evaluators and the end-to-end suites (C-018). They are not a production installation and are not a starting point for one: a real site is installed by UC-008 and secured by UC-022. No real respondent's data is entered into the stack.

### BR-005: One tag for every application image

The compose file names every Elicit application image at the same tag, read from the `ELICIT_TAG` variable and defaulting to `latest`. A site therefore runs the survey application, the console, the report service and the pedigree service from one release and cannot mix two. A pre-release version is published under its exact version only — `latest` moves only for a final version — so a beta or release candidate is run by naming it (`ELICIT_TAG=3.0.0-beta.1`, on the command line or in a `.env` file beside the compose file). The database image is versioned on its own and is not on the tag.

### BR-006: Ease of use is preferred to strictness

The stack publishes every service's port, carries documented passwords, runs an example identity provider and lets any access code into a survey. Those are choices, made so that it starts in one command, and the manual lists them rather than asking the evaluator to undo them.
