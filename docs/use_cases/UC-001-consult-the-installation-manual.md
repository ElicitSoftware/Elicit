# Use Case: Consult the Installation Manual

## Overview

**Use Case ID:** UC-001  
**Use Case Name:** Consult the Installation Manual  
**Primary Actor:** Deployment Operator  
**Goal:** Obtain a printable manual that takes an operator from an empty machine to a running, verified Elicit site, and that answers what every setting the site is configured with does, stamped with the platform version it describes.  
**Status:** Open  
**Requirements:** [FR-001, FR-003, NFR-001, NFR-006, NFR-009, C-001](../requirements.md)

## Preconditions

- The operator has the manual PDF, obtained with a release or built from this repository (UC-002).
- Nothing needs to be running. The manual is read before there is an Elicit site to serve it from.

## Main Success Scenario

1. The operator opens the manual.
2. The system shows a title page naming the platform, the version the manual was built for, and the date of that build.
3. The operator reads the orientation chapters: what the platform is made of, what the modules share, and what must exist before starting (UC-003, UC-004).
4. The operator follows the provisioning chapters in order: the database cluster (UC-005), the identity provider (UC-006), then one of the two installation paths (UC-007 or UC-008).
5. The operator completes the first sign-in and installs a survey definition (UC-009, UC-010).
6. The operator consults the configuration reference for any setting their site needs to change (UC-011, UC-012), and the branding and translations chapters for the directories their site mounts (UC-014, UC-015, UC-016).
7. The operator verifies the installation against the checks the manual gives (UC-018).
8. The system repeats the version and the build date in the footer of every page.
9. The operator holds a running, verified site and knows what every setting it runs with does.

## Alternative Flows

### A1: The operator prints the manual

**Trigger:** The operator prints or saves the document (step 1)  
**Flow:**

1. The printed copy carries the same version and build date on the title page and in every footer, so a paper copy stays attributable to the release it describes.
2. Use case continues at step 3.

### A2: The build recorded no version

**Trigger:** The manual was built outside a release, so no version or build date was supplied (step 2)  
**Flow:**

1. The system shows both the version and the date as "unknown".
2. The manual is otherwise complete and readable.
3. Use case continues at step 3.

### A3: The operator is upgrading rather than installing

**Trigger:** An Elicit site already exists (step 3)  
**Flow:**

1. The operator reads the chapter on upgrading, which names the release-specific procedure and points at `DeploymentScript.md`.
2. The manual does not restate that procedure (C-006).
3. Use case ends.

### A4: The operator needs to operate the site rather than install it

**Trigger:** The operator reaches the end of the installation (step 7)  
**Flow:**

1. The manual names the administrator's manual (Admin UC-029) and the author's manual (Author UC-039) and stops.
2. Use case ends.

## Postconditions

### Success Postconditions

- A running Elicit site whose every configured setting the operator can account for, and a manual copy attributable to one platform version.

### Failure Postconditions

- No site is installed. Nothing has been changed that a second attempt cannot repeat; the manual's procedures are re-runnable (UC-019).

## Business Rules

### BR-001: The manual is read before anything runs

The manual is a standalone document that needs no Elicit service to deliver it, because an operator reads it before any Elicit service exists. It is never packaged into or served by a module image (C-001).

### BR-002: One vocabulary with the applications

The manual uses the names the applications and the repository use: module, site, department, access code, survey definition, brand directory, translations directory. The credential a respondent enters is the access code, never a token (C-010).

### BR-003: The manual is stamped with its build

The platform version and the build date appear on the title page and in the footer of every page. A build supplying neither renders both as "unknown" rather than omitting them, so no copy is silently undated (NFR-001).

### BR-004: Procedures are self-contained

Every procedure is runnable from the manual alone: it references only commands, files and settings the manual has already given. The only external references are the Quarkus configuration index and this repository (NFR-006).

### BR-005: Specified behavior is marked as specified

A chapter documenting behavior that is specified but not implemented says so and cites the use case, so a reader never follows a procedure for a screen that does not exist (NFR-010).

### BR-007: Every screen is shown as well as told

Each stage of the manual that is a screen carries a captioned screenshot of that screen, so
that an operator can match the instruction to what is in front of them. The figures are
captured from a real installation, and the first-run figures are captured from a database
that has never been used, because those screens cannot be reproduced on an installed one.

### BR-006: Provisioning only

The manual covers provisioning a deployment. It stops where the administrator's manual and the author's manual begin (C-004).
