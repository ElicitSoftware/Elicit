# Use Case: Check Prerequisites

## Overview

**Use Case ID:** UC-004  
**Use Case Name:** Check Prerequisites  
**Primary Actor:** Deployment Operator  
**Goal:** Confirm that everything Elicit depends on exists and is reachable before the installation starts, so that a missing dependency is found in planning rather than half way through a deployment.  
**Status:** Open  
**Requirements:** [FR-007, C-007, C-008, C-009](../requirements.md)

## Preconditions

- The operator has decided which modules the site will run (UC-003).

## Main Success Scenario

1. The operator reads the prerequisite checklist.
2. The system states the database requirement: a PostgreSQL 17 cluster the operator can create roles, schemas and databases in.
3. The system states the runtime requirement for each installation path: a container runtime and compose for the container path, or a Java 25 runtime and a service manager for the module-by-module path.
4. The system states the identity requirement: an OIDC provider in which two confidential clients and four roles can be created, reachable by both the browser and the application containers at the same address.
5. The system states the mail requirement: an SMTP relay the console can send invitations and reminders through, and that the console is the only module that sends mail.
6. The system states the storage requirement: a directory for the brand and a directory for the translations, each readable by every module that mounts it.
7. The system lists the host ports each module listens on, and notes that container health checks target the container-internal port, not the published one.
8. The operator confirms each item and proceeds to provision the database (UC-005).

## Alternative Flows

### A1: A prerequisite is missing

**Trigger:** The operator cannot satisfy an item (step 8)  
**Flow:**

1. The operator provisions it, or, where the manual offers a substitute for evaluation — the bundled database image, the bundled identity provider, the bundled mail catcher — reads that substitute's entry and its warning that it carries development secrets (UC-013).
2. Use case continues at step 8.

### A2: A host port is already in use

**Trigger:** Another service listens on a port a module needs (step 7)  
**Flow:**

1. The manual states which setting changes the port a module listens on and which changes the published port in the container path, and that the identity provider's redirect URIs must be changed to match.
2. Use case continues at step 8.

### A3: The identity provider is not reachable at one address

**Trigger:** The browser and the containers reach the identity provider at different addresses (step 4)  
**Flow:**

1. The manual states why one address must work from both, what breaks when it does not — the sign-in redirect fails in a way that looks like an application fault — and gives the worked example the repository ships.
2. Use case continues at step 8.

## Postconditions

### Success Postconditions

- Every dependency the site's chosen modules need exists, is reachable, and its address and credentials are known.

### Failure Postconditions

- None. The installation has not started.

## Business Rules

### BR-001: The checklist matches the chosen modules

Only the dependencies of the modules the site will run are required. A site without the console needs no mail relay.

### BR-002: Bundled dependencies are for evaluation

The database, identity provider and mail catcher the repository bundles exist to make an evaluation site work in one command. Each ships with a documented password and none is a production dependency (UC-013).

### BR-003: Health checks address the container

In the container path the health check targets the port inside the container, which is the same for every module regardless of which host port it is published on.
