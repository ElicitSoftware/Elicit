# Use Case: Replace the Development Secrets

## Overview

**Use Case ID:** UC-013  
**Use Case Name:** Replace the Development Secrets  
**Primary Actor:** Deployment Operator  
**Goal:** Replace every credential the platform ships with a documented development value, and reconsider the settings that are deliberately permissive for evaluation, so that no part of a site runs on a password printed in this manual.  
**Status:** Open  
**Requirements:** [FR-021, FR-044](../requirements.md)

## Preconditions

- The site has installed, or is about to install, with at least one of the bundled services or with a module image whose shipped configuration still names a development password (UC-007).
- The operator can reach the database cluster, the identity provider and each module's configuration source.

## Main Success Scenario

1. The operator reads the secrets section, which lists every credential the platform ships with a documented value, where that value is set today, and which module presents it.
2. The system gives the database credentials: the login roles the bundled database image creates carry three documented passwords between the four of them, and the same passwords are named again in each module's shipped configuration, so that each one has an issuing place and one or more presenting places.
3. The system gives the account the bundled database image is initialized with, which is a superuser of the cluster and is not the role any module connects as.
4. The system gives the identity credentials: in the bundled realm the console's client carries one documented secret, the machine client used for subject import carries a second, and the bundled provider's own bootstrap administrator account is documented as well. The system states that the bundled realm as a whole — every client secret and every account in it, the authoring tool's client included — is an example for development and evaluation, and that a real site configures its own identity provider and never imports it (UC-006).
5. The system gives the remaining bundled accounts — the file-transfer account the report service uploads through, with the key pair the repository carries for it, and the mail catcher, which accepts anything and authenticates no one — and the database password the authoring tool presents, which is the survey application's.
6. The system gives the service token the compose files supply to every module so that the modules can authenticate each other in an evaluation: a documented value that names itself as not for a real site, which a module warns about at startup, and which a site replaces with a value of its own for every module at once (UC-023).
7. The operator changes each credential at the place that issues it and at every place that presents it, in the order the manual gives for that credential.
8. The operator restarts the modules whose configuration changed and confirms each one connects: the datasources through the readiness endpoint, the identity provider through a sign-in.
9. The operator reads the section's closing list of settings that are permissive by design rather than secret, decides each one for the site, and records the decision.
10. The operator confirms that no documented value remains in any source the site's configuration is read from.

## Alternative Flows

### A1: The bundled database has already been initialized

**Trigger:** The role passwords are changed after the database's first start (step 7)  
**Flow:**

1. The manual states that the image's initialization scripts run only against an empty data directory, so that editing them changes nothing on a database that already exists.
2. The manual gives the statement that changes a login role's password on a running cluster, and states that every module presenting that role's password must then be changed to match before it is restarted.
3. Use case continues at step 7.

### A2: The site brought its own database or identity provider

**Trigger:** The site runs none of the bundled services (step 2)  
**Flow:**

1. The manual states that the bundled credential does not exist for such a site, but that each module's configuration still names a password of its own, which must be set to the one the site's cluster was given (UC-005) and the one the site's provider issued (UC-006).
2. Use case continues at step 7.

### A3: A module is started without its database passwords

**Trigger:** The operator changes the database passwords across the modules (step 7)  
**Flow:**

1. The manual states that no module ships a database password: the survey application, the console and the report service each read their application password and their schema-owner password from the same two settings, and none of them carries a value of its own.
2. The manual states what a module started without them does — the database refuses it, the module logs that no password was provided, reports itself not ready, retries, and exits when its retries run out — so that a module sitting unhealthy with that message is recognized as missing the settings.
3. The manual states that a site upgrading from a release before 3.0.0, in which the survey application and the report service carried a password in their shipped configuration, must now set both.
4. Use case continues at step 7.

### A4: A service the site calls is signed by a private certificate authority

**Trigger:** A service the site calls over TLS presents a certificate the applications do not already trust (step 9)  
**Flow:**

1. The manual states that the survey application and the console verify the certificate of every service they call over TLS, and that the setting which would turn the verification off stays off.
2. The manual states that a service whose certificate is issued by a private authority is trusted by adding that authority to the application's trust store, never by turning verification off, and that a site which carried the older setting forward from a release before 3.0.0 removes it.
3. Use case continues at step 9.

### A5: The evaluation stack accepts any access code

**Trigger:** The operator reaches the settings that are permissive by design (step 9)  
**Flow:**

1. The manual states that the compose file turns on automatic registration for the survey application, so that during evaluation any access code typed in reaches a survey, and that the setting's own default is off.
2. The manual states that a site delivering a real survey leaves it off, the setting existing for an evaluation stack and for a draft being walked before it is published.
3. Use case continues at step 9.

### A6: A secret must not be written into a file the site keeps

**Trigger:** Site policy forbids a credential in the compose file or in a properties file under version control (step 7)  
**Flow:**

1. The manual states which configuration sources carry a secret to an application without it reaching a file the site keeps, and that all of them override the value built into the image (UC-011 BR-002).
2. Use case continues at step 7.

## Postconditions

### Success Postconditions

- Every credential the site presents is one the site chose, no documented value remains in any module, in the database, in the identity provider or in a bundled service the site keeps, and each permissive-by-design setting has a recorded decision.

### Failure Postconditions

- A credential has been changed where it is issued but not where it is presented, or the other way round. The module presenting it fails to connect and names the resource in its log; no data is lost, and the remedy is to finish the change rather than to reverse it.

## Business Rules

### BR-001: A documented password is a published password

Every password in this manual, in the repository's compose file and in the bundled images is public. A site that leaves one in place has no credential on that resource at all (UC-005 BR-005).

### BR-002: A credential is changed in two places

Every credential has a place that issues it and at least one place that presents it. Changing one without the other breaks the connection; the manual gives both places for every credential it lists.

### BR-003: Secrets reach an application as configuration

No credential is changed by editing or rebuilding a module image. Every one of them is a setting, supplied through a configuration source the site controls (UC-006 BR-003).

### BR-004: An accepted risk is re-confirmed, not inherited

A permissive setting the platform's own team signed off remains an open decision for each site. The manual records the decision and its date, and asks the site to make its own; it does not present the sign-off as a reason for the site not to.

### BR-005: Evaluation defaults are evaluation defaults

The compose stack is for development and evaluation (UC-007 BR-004). Every value in it that makes evaluation convenient rather than a site correct is named in this section.

### BR-006: A respondent's access code is not one of these credentials

The access code a respondent enters is issued per respondent by the console and is not a platform credential; it does not appear in this section and is not replaced here. The secrets in this section are database passwords, OIDC client secrets, service accounts and the service token the modules present to each other, which is a bearer token in the sense C-010 allows.
