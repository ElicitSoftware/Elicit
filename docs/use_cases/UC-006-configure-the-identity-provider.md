# Use Case: Configure the Identity Provider

## Overview

**Use Case ID:** UC-006  
**Use Case Name:** Configure the Identity Provider  
**Primary Actor:** Deployment Operator  
**Secondary Actor:** Identity Provider (OIDC)  
**Goal:** Create the OIDC clients and roles the console and the authoring tool check, so that users can sign in and reach exactly the parts of the platform their role permits.  
**Status:** Open  
**Requirements:** [FR-010, FR-011, C-009](../requirements.md)

## Preconditions

- An OIDC provider exists, reachable at one address from both the browser and the application containers (UC-004).
- The operator can create clients, roles and users in it.

## Main Success Scenario

1. The operator reads the identity chapter, which describes the clients and roles in provider-neutral terms and gives the repository's Keycloak realm as the worked example.
2. The system states that the console and the authoring tool each authenticate against their own confidential client, and that the survey application authenticates no one — a respondent enters an access code, not a sign-in.
3. The operator creates a confidential client for the console, with a redirect URI covering the console's address and a client secret.
4. The operator creates a confidential client for the authoring tool, with a redirect URI covering its address and a client secret.
5. The system gives the four roles and what each permits: the platform administrator role, the console user role, the authoring role, and the role a machine client uses to import subjects through the integration API.
6. The system states that the authoring role is granted explicitly and is not implied by the administrator role: an administrator who has not been given it can open the authoring tool's system pages but not a survey.
7. The operator creates the roles on the matching client and assigns them to the site's users.
8. The operator records both client secrets and the provider's issuer address for the configuration step (UC-011).
9. The operator proceeds to install (UC-007 or UC-008).

## Alternative Flows

### A1: The site uses the bundled identity provider

**Trigger:** The operator installs with the container path and keeps the bundled provider (step 3)  
**Flow:**

1. The bundled provider imports a realm carrying both clients, all four roles and three example accounts.
2. The manual states that the realm's client secret is a documented development value and names the accounts, so that a site keeping the provider replaces them (UC-013).
3. Use case continues at step 8.

### A2: The console sources roles from the database instead

**Trigger:** The site cannot manage Elicit's roles in its identity provider (step 7)  
**Flow:**

1. The manual states the setting that makes the console consult its own user-role table as a fallback, and that the setting changes only that fallback and the role-assignment section of the user editor — it does not disable OIDC and does not affect roles the provider supplies.
2. The manual states that under this mode the identity provider's account names must match the console's user records.
3. Use case continues at step 8.

### A3: The provider is reached at different addresses by browser and container

**Trigger:** Sign-in redirects fail after installation (step 9)  
**Flow:**

1. The manual states that the issuer address configured for an application must resolve to the same provider from the browser and from inside the container, explains the symptom — the browser cannot follow the redirect and the application appears to hang or lands on the wrong service — and gives the address the bundled example uses and why.
2. Use case continues at step 8.

### A4: A machine client imports subjects

**Trigger:** The site integrates an external system with the console's subject API (step 5)  
**Flow:**

1. The manual states that the integration role is checked on bearer tokens, that the console accepts both browser sign-ins and bearer tokens on the same client, and that a service account holding only the integration role reaches the import endpoints and nothing else.
2. Use case continues at step 8.

## Postconditions

### Success Postconditions

- Two confidential clients exist with recorded secrets, four roles exist, and at least one account holds the administrator role so that the first sign-in can proceed (UC-009).

### Failure Postconditions

- No client or role is created, or a partial set is; nothing in Elicit is affected, since no module has started.

## Business Rules

### BR-001: One client per signing-in application

The console and the authoring tool each have their own client so that a role granted for one does not reach the other.

### BR-002: The authoring role is never implied

Holding the administrator role grants no authoring access. The authoring role is granted explicitly (Author UC-002).

### BR-003: Respondents do not sign in

The survey application has no OIDC client. A respondent reaches a survey with an access code issued by the console, which is not a credential in the identity provider (C-010).

### BR-004: Elicit checks roles, the provider issues them

No module creates roles in the identity provider. Every module only checks the roles a token carries, with the one documented exception of the console's database fallback (A2).

### BR-005: Secrets are configuration, not code

Client secrets reach an application only as configuration. The manual identifies the setting that carries each one (FR-021).
