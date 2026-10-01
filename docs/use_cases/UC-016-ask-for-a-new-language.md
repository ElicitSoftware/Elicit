# Use Case: Ask for a New Language

## Overview

**Use Case ID:** UC-016  
**Use Case Name:** Ask for a New Language  
**Primary Actor:** Deployment Operator  
**Goal:** Obtain a language the release does not yet carry, and know what to expect, so that the operator neither looks for a file to edit nor promises a date they do not control.  
**Status:** Open  
**Requirements:** [FR-028, FR-029, C-012, C-015](../requirements.md)

## Preconditions

- The operator knows which of the release's languages the site already offers (UC-015).
- The operator knows which language is wanted, and for which of the applications.

## Main Success Scenario

1. The operator reads the language chapter, which states that a language is curated by ElicitSoftware and packaged into the applications, and that a deployment can neither add one nor patch one.
2. The system states the reason rather than only the rule: a translation and the code that renders it are released together, so a site always renders the wording that version was built and tested with, and "which translation is this site running?" is answered by the version tag alone.
3. The operator asks ElicitSoftware for the language, naming it by its BCP-47 tag.
4. The system states what ElicitSoftware does with that request: each application's `i18n/TRANSLATION_REQUEST.md` is handed to a translator or an AI agent (UC-017), the returned file is added to both applications, and the build refuses the release if they disagree about which languages they carry.
5. The system states that the language arrives in a later version, and that the site takes it by upgrading rather than by editing anything.
6. After upgrading, the operator adds the tag to `i18n.bundled.locales` for each application that should offer it (UC-015) and restarts.

## Alternative Flows

### A1: A shipped language needs correcting

**Trigger:** A text in a language the release carries is wrong at this site (step 1)  
**Flow:**

1. The manual states that this is the same request as a new language: the correction is made in the applications and arrives in a later version.
2. The manual states that there is no local override — a site cannot patch one text — and gives the reason: a site-local edit is invisible to everyone supporting the site, and reproduces the drift the packaged translations exist to prevent.
3. Use case ends.

### A2: A language is withdrawn from a site

**Trigger:** The site should stop offering a language it currently offers (step 1)  
**Flow:**

1. The manual states that this needs no release: remove the tag from `i18n.bundled.locales` and restart (UC-015).
2. The manual states that the language remains in the image, unreachable, and that re-offering it later is the same one-line change.
3. Use case ends.

### A3: The language is needed sooner than the next release

**Trigger:** The site cannot wait for a scheduled version (step 3)  
**Flow:**

1. The manual states plainly that there is no supported way to shorten this, and that the release cadence is the cost of a translation that always matches the code it ships with.
2. The manual directs the operator to ElicitSoftware to discuss the schedule rather than to a workaround.
3. Use case ends.

### A4: The in-application Languages screen — not yet available

**Trigger:** The operator looks for a screen that manages languages (step 1)  
**Flow:**

1. The manual states that a Languages screen is specified for the console (Admin UC-027) and is not built.
2. The manual states that it was specified against a writable translations directory that no longer exists, so what it will manage is the offered-language list rather than files, and that until it exists the setting is the operative route.
3. Use case ends.

## Postconditions

### Success Postconditions

- The operator knows the language will arrive in a release, has asked for it, and knows the one setting that will offer it once the site upgrades.

### Failure Postconditions

- The site continues to offer the languages it already offered. Nothing about asking for a language changes a running deployment.

## Business Rules

### BR-001: Languages come from a release, not from a deployment

A language is curated by ElicitSoftware and packaged into every application that needs it. A deployment can neither add one nor patch one; it chooses among what the release carries (UC-015, C-012).

### BR-002: A language is added to both applications together

Survey and Admin gain a language in the same release, and the build refuses to produce one where they disagree. A language one application had and another lacked could not be served: a survey's content is only shown in a language the site also offers for the application's own texts, so an author could otherwise publish content no respondent could read (C-015).

### BR-003: A correction is a release, like a new language

There is no site-local override for a single text. The alternative — a wording that differs from the release at one site, invisible to everyone supporting it — is the drift that packaging the translations exists to prevent.

### BR-004: Withdrawing a language needs no release

Removing a tag from the offered list is a configuration change and takes effect on restart. The language stays in the image, unreachable, and can be re-offered by the same one-line change.
