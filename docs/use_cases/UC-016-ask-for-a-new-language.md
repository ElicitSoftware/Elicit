# Use Case: Ask for a New Language

## Overview

**Use Case ID:** UC-016  
**Use Case Name:** Ask for a New Language  
**Primary Actor:** Deployment Operator  
**Goal:** Obtain a language the curated set does not include, or a correction to one it does, either from ElicitSoftware or by supplying the file itself, and know what each route means for the site.  
**Status:** Open  
**Requirements:** [FR-028, FR-029, C-012, C-015](../requirements.md)

## Preconditions

- The operator knows which languages the site already mounts and offers (UC-015).
- The operator knows which language is wanted, and for which of the applications.

## Main Success Scenario

1. The operator reads the language chapter. It states that ElicitSoftware maintains a curated set of translations beside each release (`elicit-i18n/`), and that a site may also mount files of its own.
2. The system states the difference between the two routes:
   - **The curated set** is reviewed, versioned with the release, and checked against it: the build refuses a release where Survey and Admin disagree about which languages the set holds.
   - **A site's own file** takes effect on the next restart, and the site owns it.
3. The operator asks ElicitSoftware for the language, naming it by its BCP-47 tag.
4. The system states what ElicitSoftware does with that request:
   - each application's `i18n/TRANSLATION_REQUEST.md` is handed to a translator or an AI agent (UC-017);
   - the returned files are added to the curated set for both applications.
5. The system states that the language arrives with a later version's curated set, and that the site takes it by mounting that set.
6. The operator mounts the new files and, where `i18n.offered.locales` is set, adds the tag to it for each application that should offer it (UC-015). Then the operator restarts.

## Alternative Flows

### A1: A curated language needs correcting at this site

**Trigger:** A text in a mounted language is wrong at this site (step 1)  
**Flow:**

1. The manual states that the site may correct it at once. A copy of the file, with the corrected text, mounted in place of the curated one, takes effect on restart. A file holding only the corrected texts also works, but then every other text in that language falls back to English.
2. The manual states the cost: everyone supporting the site sees a wording that differs from the release, and the next curated set does not carry the correction unless it is reported. The manual asks for the correction to be sent to ElicitSoftware as well.
3. Use case ends.

### A2: A language is withdrawn from a site

**Trigger:** The site should stop offering a language it currently offers (step 1)  
**Flow:**

1. The manual states that this needs no release. Either remove the file from the mount, or remove the tag from `i18n.offered.locales`, and restart (UC-015).
2. The manual states that re-offering it later is the same change in reverse.
3. Use case ends.

### A3: The language is needed sooner than the next release

**Trigger:** The site cannot wait for a scheduled version (step 3)  
**Flow:**

1. The manual states that the site may commission the translation itself (UC-017) and mount the returned file. The language is then offered from the next restart.
2. The manual states that the file is the site's own until ElicitSoftware adds the language to the curated set, and that a site's file made from one release's request document lacks the texts a later release adds, which then appear in English.
3. Use case ends.

### A4: The in-application Languages screen — not yet available

**Trigger:** The operator looks for a screen that manages languages (step 1)  
**Flow:**

1. The manual states that a Languages screen is specified for the console (Admin UC-027) and is not built, so the mounted directory and the setting are the operative route.
2. Use case ends.

## Postconditions

### Success Postconditions

- The operator has the language, either from the curated set or as a file of the site's own. The operator knows how to offer it, and knows who maintains the wording.

### Failure Postconditions

- The site continues to offer the languages it already offered. Nothing about asking for a language changes a running deployment.

## Business Rules

### BR-001: ElicitSoftware curates; a site may supply

ElicitSoftware maintains the curated translations beside each release, and they are not part of any image. A site mounts that set, part of it, or files of its own, and can add or correct a language without a release (UC-015, C-012).

### BR-002: The curated set gains a language for both applications together

The curated set gains a language for Survey and Admin together, and the build refuses a release where the two disagree. A language one application had and the other lacked would offer respondents and staff different choices, and a site mounting the set would have to notice and repair that (C-015).

### BR-003: A site's own wording is the site's to maintain

A file a site supplies or edits is invisible to everyone supporting the site, and it does not follow the release. When the release adds texts, they appear in English until the file is updated. The site keeps such a file with its configuration (UC-011) and reports corrections upstream.

### BR-004: Withdrawing a language needs no release

Removing a language is a change to the mount or to the offered list, and takes effect on restart.
