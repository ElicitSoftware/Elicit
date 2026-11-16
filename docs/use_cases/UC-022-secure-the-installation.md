# Use Case: Secure the Installation

## Overview

**Use Case ID:** UC-022  
**Use Case Name:** Secure the Installation  
**Primary Actor:** Deployment Operator  
**Goal:** Put in place every control that protects a site — what reaches the Internet, transport security, the identity provider and its accounts, access codes, the database, secrets, what the site loads, and what it logs — and work through a checklist that records each one, so that a site holding respondents' answers is hardened deliberately rather than by assumption.  
**Status:** Open  
**Requirements:** [FR-043, FR-044, C-017, C-019, C-020](../requirements.md)

## Preconditions

- The site is installed by the path a production site uses (UC-008), against its own database (UC-005) and its own identity provider (UC-006).
- The development secrets have been replaced (UC-013).
- The operator controls the site's reverse proxy, its network rules and each module's configuration source (UC-011).

## Main Success Scenario

1. The operator reads the security chapter, which opens by stating what the applications leave to the deployment: a private network, a reverse proxy that terminates TLS, an identity provider, and a place to keep secrets.
2. The system states that the bundled compose file and the services bundled with it — database, identity provider, mail catcher, SFTP server and trace collector — are for development and evaluation, and are not a production installation.
3. The operator reads which service is reached by whom, and publishes only the survey application and the console, each through the reverse proxy. Report services, the database, the mail relay and the trace collector stay on the private network.
4. The operator configures the proxy to refuse the paths that serve the site's own applications rather than its users: on the survey application every `/api` path except the PDF download, and on both applications everything under `/q`.
5. The operator makes the applications reachable only through the proxy, because they trust the forwarded headers it sets, and limits the rate of requests per client address.
6. The operator sets transport security: `https` for every address a browser uses, the response headers the chapter lists, and a verified, encrypted connection for each outbound dependency the site reaches across a network it does not control.
7. The operator configures the identity provider and the accounts: the site's own provider, an exact redirect address, a client that allows the authorization code flow only, failed sign-ins limited and sign-ins recorded by the provider, roles granted narrowly, the machine role held by a machine client only, the seeded accounts renamed, and departed people removed at the provider.
8. The operator protects the access code: automatic registration off, the invitation link over `https`, and the logs that record the link's address treated as holding credentials.
9. The operator protects the database: no address outside the application hosts, the connection roles the manual prescribes, none of them a superuser, the schema owner's password held as the most sensitive secret of the site, and backups protected as the database is.
10. The operator supplies every secret through a configuration source that keeps it out of files under version control, and knows the two places each one is changed.
11. The operator gives every module the site's own service token, so that the paths one module serves for another answer only a module of the site, and lists as trusted only the origins of services the site runs and trusts (UC-023).
12. The operator decides who may change what the site loads: definition files applied only from authors the site trusts, with the addresses each one carries reviewed, and the brand and translations directories writable by operators only.
13. The operator restricts the health and metrics endpoints, the logs and the traces to the site's own network and people.
14. The operator runs a named release of every module and follows the release's upgrade procedure when a new one is published.
15. The operator works through the chapter's checklist, each line of which names the section that explains it, and records the outcome. The service token has a line of its own.

## Alternative Flows

### A1: The site does not run the Family History reports

**Trigger:** The site runs neither report service (step 3)  
**Flow:**

1. The manual states that the lines of the checklist about report services and document filing do not apply to such a site.
2. Use case continues at step 4.

### A2: A machine client uses the integration API

**Trigger:** The site imports subjects through the console's API (step 7)  
**Flow:**

1. The manual states that the client is a client of its own in the identity provider, with its own secret, holding the importer role and nothing else.
2. Use case continues at step 7.

### A3: A service is signed by a private certificate authority

**Trigger:** An outbound dependency presents a certificate the Java runtime does not trust (step 6)  
**Flow:**

1. The manual states that the authority is added to the trust store and that certificate verification is never turned off (UC-012).
2. Use case continues at step 6.

### A4: The schema owner is still a superuser

**Trigger:** The cluster was prepared by the role script of an earlier release, and the checklist's query shows the schema owner or the owner group as a superuser (step 9)  
**Flow:**

1. The manual states that no role Elicit prescribes is a superuser (C-020), and points at the upgrade notes for the statements that narrow the roles of a cluster that already exists (UC-005 A3).
2. Use case continues at step 9.

## Postconditions

### Success Postconditions

- Every line of the checklist is done or recorded as not applying, and the site's exposure is the survey application and the console behind the proxy and nothing else.

### Failure Postconditions

- A control is missing and the checklist says which. Nothing the chapter asks for changes data, so the remedy is to finish the list.

## Business Rules

### BR-001: The chapter describes controls, never weaknesses

The manual is public. The chapter states what a site does to be secure and why; it does not catalog defects, and it names no finding of any review.

### BR-002: Only what exists is described

The chapter describes the controls of the version the manual is stamped with (NFR-010). A control that a later release adds is added to the chapter with that release.

### BR-003: The bundled stack is not a production installation

The compose file and its bundled services exist so that a developer or an evaluator has a working site in one command. Their defaults favor ease of use, and the chapter says so rather than hardening them.

### BR-004: Every checklist line has a description

No line of the checklist stands alone: each cites the section that says what the control is and why it matters, so that the list can be worked by someone who has not read the chapter in order.

### BR-005: The proxy refuses what is not for users

A path that serves the site's own applications is refused at the reverse proxy whether or not the application behind it also checks the caller. The applications do check: those paths require the service token (UC-023). The proxy rule stays, as the first of two locks.
