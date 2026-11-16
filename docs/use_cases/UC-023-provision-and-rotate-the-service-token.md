# Use Case: Provision and Rotate the Service Token

## Overview

**Use Case ID:** UC-023  
**Use Case Name:** Provision and Rotate the Service Token  
**Primary Actor:** Deployment Operator  
**Goal:** Give every module of a site the one secret value the modules authenticate each other with, name the addresses that value may be sent to, and replace it when it has to change, so that an endpoint one module serves for another answers only a caller that holds the site's token.  
**Status:** Open  
**Requirements:** [FR-044, NFR-014, C-019, C-017](../requirements.md)

## Preconditions

- The operator is installing a site by the path a real site uses (UC-008), or already runs one.
- The operator controls each module's configuration source and can supply a secret through it (UC-011, UC-013 A6).
- The operator knows the addresses the site's survey definitions name for their report services and post-survey actions, or can read them in Author or on the console's Connections screen.

## Main Success Scenario

1. The operator reads the service token section, which states what the token is: one value per deployment, which a module presents as a bearer credential when it calls another module and checks on the paths it serves for other modules.
2. The system states which paths each module protects — on the survey application every `/api` path except the PDF download, on the Family History report service everything outside `/q`, on the pedigree service everything but its health endpoint — and that the health endpoints stay open.
3. The operator generates a value with the command the manual gives, which produces 64 hexadecimal characters.
4. The operator supplies the value to every module the site runs — the survey application, the console, and the report service and the pedigree service where the site runs them — as the environment variable `ELICIT_SERVICE_TOKEN`, from the place the site keeps its secrets.
5. The system states which addresses a module sends the token to without being told: the address of each module it is configured with. The console's address for the survey application and the report service's address for the pedigree service are of this kind.
6. The system states that an address a survey definition carries is never sent the token unless the operator has listed its origin — scheme, host and port — as trusted, and gives the Family History reports as the example: the origin at which the survey application and the console reach the report service.
7. The operator sets the trusted origins on the survey application, the console and the report service, as `ELICIT_SERVICE_TRUSTED_ORIGINS`.
8. The operator starts the modules. Each reports ready, and the readiness check named `service-token` passes.
9. The operator confirms the token is enforced with the requests the verification chapter gives: without the token a protected path answers `401`, with it the request is served, and health answers either way (UC-018).
10. The operator records where the token is kept and who may read it, as for every other secret of the site (UC-022).

## Alternative Flows

### A1: A module is started without the token

**Trigger:** A module's configuration does not carry the variable, or carries it empty (step 8)  
**Flow:**

1. The module starts, logs one warning naming the variable, and reports not ready: its `service-token` check fails with the reason "ELICIT_SERVICE_TOKEN is not set". The status script prints that reason.
2. The module answers `503` on the paths it protects and makes no call to another module; the pages it serves to people are unaffected. The pedigree service has no readiness reason to give: its health endpoint answers, and its drawing endpoint answers `503`.
3. The operator supplies the value and restarts the module.
4. Use case continues at step 8.

### A2: The token is shorter than 32 characters

**Trigger:** The value supplied is set but too short (step 8)  
**Flow:**

1. The module refuses to start. Its log names the variable and the length required, and neither the value nor its length.
2. The operator generates a value as step 3 gives and starts the module again.
3. Use case continues at step 8.

### A3: A report address is not on the trusted list

**Trigger:** A report shows an error card, or a post-survey action's deliveries fail with `401`, though the service is running (step 9)  
**Flow:**

1. The manual states the cause: the address in the survey's definition has an origin the calling module was not told to trust, so the call was made without the token and the service refused it.
2. The operator reads the address on the console's Connections screen or in Author, and either adds its origin to the trusted origins of the survey application, the console and the report service, or changes the address in Author to one whose origin is already listed and applies the new export (UC-021).
3. The operator restarts the modules whose trusted origins changed.
4. Use case continues at step 9.

### A4: The token is rotated

**Trigger:** Someone who knew the token has left, the token may have been disclosed, or site policy sets a date (any time after step 10)  
**Flow:**

1. The operator generates a new value (step 3) and replaces the old one for every module of the site.
2. The operator restarts all of the modules together rather than one at a time.
3. The manual states what happens between the first restart and the last: a call from a module on one value to a module on the other answers `401`. A definition applied in that time reports that the reporting schema was not rebuilt, a report shows its error card, and a post-survey action is retried on its schedule. Nothing is lost.
4. The manual states that there is no second, "previous" token: the old value stops working when the last module restarts.
5. Use case continues at step 9.

### A5: The development default is still in place

**Trigger:** A module runs with the value the compose files ship (step 8)  
**Flow:**

1. The module logs a warning at startup that it is running with the published development token.
2. The manual states that the value is printed in the compose file and in the manual, so that a module holding it accepts a call from anyone who can reach it, and that it exists so that the evaluation stack starts in one command (UC-007).
3. On an evaluation stack the operator leaves it. On a site the operator generates a value of the site's own.
4. Use case continues at step 3.

### A6: The site lists a report service of its own

**Trigger:** The site runs a custom report service and wants its calls authenticated (step 6)  
**Flow:**

1. The manual states that a listed service receives the platform's token on every call, and that whoever holds the token can call every protected path of every module.
2. The operator lists the service's origin only if the site trusts the service as much as it trusts the survey application itself, and gives the service the token to check. Otherwise the service is left off the list, is called without a credential, and is protected by the network alone (UC-020 BR-009).
3. Use case continues at step 7.

### A7: A trusted origin is malformed

**Trigger:** An entry of the trusted origins carries a path, a query or user information, or lacks a scheme or a host (step 8)  
**Flow:**

1. The module refuses to start, and its log says that the trusted origins are at fault.
2. The operator reduces the entry to scheme, host and port and starts the module again.
3. Use case continues at step 8.

## Postconditions

### Success Postconditions

- Every module of the site holds the same token, of the site's own making, and no module holds the development default.
- A protected path answers a caller only when it presents the token; health endpoints answer without it.
- The token is sent to the modules a module is configured with and to the origins the operator listed, and to no other address.

### Failure Postconditions

- A module without the token is not ready and says why; a module with a wrong one is refused with `401` by the modules it calls. No data is lost in either case, and supplying the right value and restarting is the whole fix.

## Business Rules

### BR-001: One token per deployment

Every module of one site holds the same value. A second site has its own, and so does an Author installation run apart from a site.

### BR-002: The token is a bearer credential in the Authorization header

A caller presents it as `Authorization: Bearer <token>`, never in an address and never in another header. A receiver compares it in constant time.

### BR-003: A receiver without a token refuses

A module that holds no token never serves a protected path unauthenticated: it answers `503`, reports not ready with the reason, and makes no service call of its own. A missing or wrong token on a request answers `401`, and the two are indistinguishable to the caller.

### BR-004: The token is at least 32 characters

A shorter value stops the module at startup. An unset value does not (BR-003).

### BR-005: The token goes only where configuration says

A module sends the token to the origin of an address its own configuration names, and to an origin listed in `elicit.service.trusted-origins`. Origins are compared exactly — scheme, host and port — with no wildcard and no prefix or suffix matching. An address that arrives in a definition file is not trusted for arriving there. A call that carries the token follows no redirect.

### BR-006: A post-survey action's own credential comes first

Where the site has configured a bearer token for a post-survey action, that token is sent and the service token is not, whatever the action's address. Otherwise the service token is sent if the address's origin is trusted, and nothing is sent if it is not.

### BR-007: The token is never shown

No log line, trace, diagnostics page or error body carries the token or a header presented to a module. Diagnostics say whether it is set (NFR-014).

### BR-008: The token says a module is calling, not for whom

A caller that holds the token may ask a receiver for any respondent's report or any survey's reporting schema. Who may see which respondent is decided by the calling application before it calls, and the manual says so in the section's account of what the token does not do.

### BR-009: Rotation is all at once

There is one token and no period in which two are accepted. Every module is given the new value and all are restarted together.

### BR-010: The network rule still holds

The token is a second lock, not a replacement for the first: report services stay on the private network, and the reverse proxy still refuses the paths that are not for users (UC-022 BR-005).
