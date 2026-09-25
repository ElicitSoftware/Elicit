# Use Case: Mount the Translations Directory

## Overview

**Use Case ID:** UC-015  
**Use Case Name:** Mount the Translations Directory  
**Primary Actor:** Deployment Operator  
**Goal:** Give the survey application, the console and the authoring tool a directory of translation files, so that the site can offer languages the images do not ship and can predict what a reader sees when a file translates only part of an application.  
**Status:** Open  
**Requirements:** [FR-026, FR-027, FR-031](../requirements.md)

## Preconditions

- The applications are installed by one of the two paths (UC-007 or UC-008) and the operator knows how a setting reaches them (UC-011).
- The operator knows which languages the site is to offer, and for which of the three applications.

## Main Success Scenario

1. The operator reads the translations chapter, which states that every image ships English only, that every other language is a file in a mounted directory, and that the language selector in the header appears only once more than one language is available — so an English-only site never shows one.
2. The system gives the directory layout: an optional `i18n-config.json` at the root of the mount, and one sub-directory per application holding `translations.properties` and a `translations_<tag>.properties` for each further language.
3. The system states that the sub-directory an application reads is named by `i18n.app.name` — `survey`, `admin` or `author` — that the authoring preview instance reads `survey` like the survey application, and that a value containing a path separator is rejected rather than followed.
4. The operator takes the repository's translations directory as the starting point, which already carries a copy of each application's English file together with Latin American Spanish and Arabic, and edits it in place or copies it to the site's own location.
5. The operator points each application at the directory with `i18n.file.system.path` — which defaults to `/i18n`, and which the compose file sets to `/opt/i18n` with a read-only volume — and gives the survey application, the console, the authoring tool and the authoring preview the same mount.
6. The system gives the resolution order, which runs per key rather than per file: the bundle inside the image, then the local directory named by `i18n.local.path` (default `i18n`), then the mount, with a later tier winning for any key it defines; the exact language tag then falls back to the language alone, and then to English; and a key defined in no tier renders as `!key!` and is logged once rather than rendering blank.
7. The system states that the languages an application offers are the union of `i18n.bundled.locales` — `en` in every image — and every `translations_*.properties` found in the local directory and on the mount, so a language that exists only on the mount is offered in the selector like any other.
8. The system states plainly what this mechanism does not reach: survey content held in the database — question and answer text, answer options, section and step names, report bodies and message templates — is not translated by it and appears in the language it was authored in, whatever language a reader selects.
9. The operator restarts each application, confirms that the selector lists the expected languages, and proceeds to add, replace or remove a language (UC-016).

## Alternative Flows

### A1: A mounted file translates only part of an application

**Trigger:** The site mounts a file carrying a few keys rather than a complete language (step 6)  
**Flow:**

1. The manual states that a partial file is an override rather than an error: the keys it carries win, and every key it omits keeps the value from the tier below, ending at English.
2. The manual states the visible consequence — a page mixing the new language with English — and that this is the intended behaviour, not a sign of a broken mount.
3. Use case continues at step 7.

### A2: Only one application is translated

**Trigger:** The site offers a language to respondents but not to its own staff (step 4)  
**Flow:**

1. The manual states that each application reads only its own sub-directory, so a language placed in `survey/` alone reaches the survey application and neither the console nor the authoring tool.
2. The manual states that a sub-directory an application does not read costs it nothing, so one directory can be mounted everywhere regardless of which applications it covers.
3. Use case continues at step 5.

### A3: The mount is not read

**Trigger:** The selector still offers English alone after the restart (step 9)  
**Flow:**

1. The manual gives the causes actually seen: a configured path that does not match the volume's target, files placed at the root of the mount rather than in an application's sub-directory, and a sub-directory whose name does not match the application's `i18n.app.name`.
2. The manual states that none of these produces an error, because a directory that holds no readable file is indistinguishable from one that was never mounted.
3. Use case continues at step 5.

### A4: A language is built into the site's own image

**Trigger:** The site prefers to carry a language with the service rather than in a mount (step 5)  
**Flow:**

1. The manual states that the local tier, named by `i18n.local.path` and read relative to the service's working directory, holds the same layout and is consulted between the image's own bundle and the mount.
2. The manual states that a language present in both tiers resolves from the mount, so a mount always remains able to correct a text the site shipped.
3. Use case continues at step 6.

### A5: The site installs without containers

**Trigger:** The applications run as JVM services rather than containers (step 5)  
**Flow:**

1. The manual states that `i18n.file.system.path` is an ordinary configuration setting reached by any Quarkus configuration source, and that it names a directory on the host laid out exactly as the mount is.
2. Use case continues at step 6.

## Postconditions

### Success Postconditions

- Each application reads its own sub-directory of the mounted directory, offers English plus every language found there, and resolves each key from the highest tier that defines it.

### Failure Postconditions

- Each application runs in English alone and serves pages normally. A directory that cannot be read is not an error and does not prevent an application from starting.

## Business Rules

### BR-001: English is always available and is the last fallback

Every image carries the complete English texts and offers English whatever else is mounted. Any key a selected language does not define is rendered in English rather than left blank, so a page is never partly empty.

### BR-002: Resolution is per key, not per file

A mounted file does not replace the file below it; it overrides the keys it defines. This is what makes a one-key file a valid deployment artefact, and it is why a language cannot be "partly installed" in a way that breaks a page.

### BR-003: Each application reads only its own sub-directory

An application never reads another application's texts. Making a language available to the whole site therefore means placing a file in each of the sub-directories the site runs.

### BR-004: Survey content is not translated by this mechanism

Only the applications' own chrome — navigation, buttons, labels, notifications, validation messages, and document headers and footers — is translated here. Question text, answer options, section and step names, report bodies and message templates are stored in the database and appear as authored (FR-031).

### BR-005: Translation files are read server-side only

Nothing in the translations directory is served over HTTP. The directory exists for the applications to read on startup, and mounting it exposes no new endpoint.

### BR-006: The language a reader sees is chosen per session

A `?lang=<tag>` parameter on a route wins, then the language remembered in the browser session, then the browser's own preference negotiated against the offered languages, then English. The choice lives in the session only; it is never stored against a respondent or a user account, and it never crosses sessions (Admin UC-026, Author UC-040).
