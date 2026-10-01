# Use Case: Complete the First Sign-In

## Overview

**Use Case ID:** UC-009  
**Use Case Name:** Complete the First Sign-In  
**Primary Actor:** Deployment Operator  
**Secondary Actor:** Identity Provider (OIDC)  
**Goal:** Take a freshly installed console from its first sign-in to a usable state, by creating the department without which no console screen works and replacing the two accounts the migrations seed.  
**Status:** Open  
**Requirements:** [FR-014](../requirements.md)

## Preconditions

- Every module the site runs is installed and reports healthy (UC-007 or UC-008).
- The identity provider authenticates at least one account holding the administrator role, and the console holds a user record under that same username (UC-006).

## Main Success Scenario

1. The operator reads the first sign-in chapter, which states what a new installation actually contains: two accounts created by the migrations so that the console can be signed into at all, no department, no message template and no survey.
2. The operator opens the console and signs in through the identity provider as an account holding the administrator role.
3. The system finds no department on that account and shows a dialog that blocks every console screen, cannot be dismissed, explains the condition and offers one action leading to the Departments screen (Admin UC-028).
4. The operator follows that action and creates the site's first department, with the name, code and sending address the site's invitation and reminder mail will carry.
5. The system assigns the new department to the account that created it as part of saving it, so the dialog is gone on the next screen the operator opens and the console becomes usable, with no restart and no second sign-in (Admin UC-028 BR-113).
6. The system carries a warning on every console screen naming the two seeded accounts, because their usernames are published in this manual and in the repository, and offers the Users screen as the place to act (Admin UC-021).
7. The operator renames both seeded accounts on the Users screen to the site's own people, and creates any further accounts the site needs, assigning each of them to a department.
8. The system stops showing the warning on the next screen opened once neither seeded username remains.
9. The operator proceeds to installing a survey definition (UC-010).

## Alternative Flows

### A1: The first account to sign in is not an administrator

**Trigger:** The account signing in holds the console user role only (step 2)  
**Flow:**

1. The dialog states that an administrator must assign the account to a department and offers signing out as its only action, since creating a department is not that account's to perform (Admin UC-028 A1).
2. The operator signs in instead as an account holding the administrator role.
3. Use case continues at step 2.

### A2: The installation already carries a department

**Trigger:** The console is installed over a database created before the seeded department was withdrawn, or over an existing site's database (step 3)  
**Flow:**

1. The manual states that an applied migration is never re-run, so such a database keeps the department it was seeded with and no dialog appears.
2. Use case continues at step 6.

### A3: The identity provider authenticates an account the console does not know

**Trigger:** Sign-in succeeds but the console holds no user record under that username (step 2)  
**Flow:**

1. The console shows no department dialog and states instead that the account is authenticated but has no active application record, and to contact an administrator, because the condition is a missing account and not a missing department (Admin UC-001, UC-028 A4).
2. The operator signs in as a seeded account, creates the console record for the site's own account on the Users screen, and assigns it a department.
3. Use case continues at step 2.

### A4: The site sources roles from the console's own tables

**Trigger:** The console consults its user-role table rather than the token's roles (step 7, UC-006 A2)  
**Flow:**

1. The manual states that a rename under this mode must be made in both places, because the console matches a signed-in principal to its record by username: renaming only the console record locks the account out, and renaming only the identity provider leaves the seeded record in place and the warning with it (Admin UC-021 BR-088).
2. Use case continues at step 7.

### A5: Sign-in does not complete

**Trigger:** The browser is not returned to the console after authenticating (step 2)  
**Flow:**

1. The manual states the usual cause — the issuer address the application is configured with does not resolve to the same provider from the browser and from inside the container — and points at the identity provider chapter (UC-006 A3) and the failure table (UC-019).
2. Use case continues at step 2.

## Postconditions

### Success Postconditions

- At least one department exists, the administrator who created it is assigned to it, and no console screen is blocked.
- Neither seeded username remains, and the console shows no default-account warning.
- The console is usable for its own administration — departments, users, message templates — although no survey is installed yet (UC-010).

### Failure Postconditions

- No department exists and every administrator who signs in is held at the dialog, with signing out as the only action left. Nothing is corrupted: the remedy stays reachable at the next sign-in, and the seeded accounts remain the way in.

## Business Rules

### BR-001: A new installation seeds accounts and nothing else

The migrations create the two accounts so that a fresh installation can be signed into. They create no department, no message template — a template cannot exist without a department — and no survey. Everything else a site runs on is created through the console or applied to it (Admin UC-028 BR-115).

### BR-002: The first department is created through the console

Creating a department in the console assigns it to the administrator creating it, in the same save. A department inserted directly into the database, as the older deployment notes describe, is assigned to nobody, so an operator taking that route must assign it as a second, separate step or the dialog stays.

### BR-003: This is the one notice that blocks

A missing survey and a seeded account leave every console screen usable and are reported as warnings. A missing department leaves nothing usable, so it is reported as a modal dialog instead. The screens that carry the remedy are exempt from the block, and signing out is offered in every variant, so a deployment can never be made unrecoverable by it (Admin UC-028 BR-110, BR-111, BR-112).

### BR-004: Both conditions clear without a restart

Whether the signed-in account has a department, and whether a seeded username still exists, are re-established as the console is used rather than captured at sign-in or at service start. A department created or an account renamed takes effect on the next screen opened, and never requires the service to be restarted or the session to be re-established (Admin UC-028 BR-114, UC-021 BR-086).

### BR-005: The seeded accounts are renamed, not deactivated

The check is by username, so deactivating a seeded account leaves the warning in place and leaves a published username in the database. Renaming is the remedy (Admin UC-021 BR-085).

### BR-006: A usable console is not a complete installation

Completing this use case makes the console usable; it does not give the site a survey. The console states plainly that no survey is installed and offers where to apply one, rather than pretending to be finished (Admin UC-019, UC-010).
