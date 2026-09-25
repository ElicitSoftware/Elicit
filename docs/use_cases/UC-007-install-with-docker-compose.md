# Use Case: Install with Docker Compose

## Overview

**Use Case ID:** UC-007  
**Use Case Name:** Install with Docker Compose  
**Primary Actor:** Deployment Operator  
**Goal:** Bring up a complete Elicit site with one compose file, so that the platform can be evaluated or run without assembling each module by hand.  
**Status:** Open  
**Requirements:** [FR-012](../requirements.md)

## Preconditions

- A container runtime and compose are installed (UC-004).
- The operator has the umbrella repository checked out, which carries the compose file, the default brand and the default translations.

## Main Success Scenario

1. The operator reads the compose chapter, which names every service the file defines, the host port each publishes, the volumes each mounts and what each depends on.
2. The system distinguishes the application services from the supporting services the file bundles for evaluation — the database, the identity provider, the mail catcher, the file-transfer server and the trace collector.
3. The operator edits the compose file for the site: the database passwords and the client secret (UC-013), the brand directory (UC-014), the translations directory (UC-015) and any setting from the configuration reference (UC-011).
4. The operator brings the stack up in one command.
5. The system starts the database, waits for it to become healthy, then starts the survey application, which creates and migrates the shared schema.
6. The system starts the remaining services as their dependencies become healthy: the console, the authoring tool's database instance and the authoring tool, and the two optional services if the site runs them.
7. The operator watches the services become healthy and reads the status script's output.
8. The operator proceeds to the first sign-in (UC-009).

## Alternative Flows

### A1: The site brings its own database or identity provider

**Trigger:** The site has a PostgreSQL cluster or an OIDC provider of its own (step 3)  
**Flow:**

1. The operator removes the bundled service from the compose file and points the applications at the site's own, using the datasource and OIDC settings from the configuration reference.
2. Use case continues at step 4.

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

### A4: The site does not run the report service

**Trigger:** The site does not run the Family Health History Survey (step 6)  
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

### BR-004: The compose file is an evaluation starting point

The file is a working site and a worked example. Every value in it that a production site must change is identified in the configuration reference.
