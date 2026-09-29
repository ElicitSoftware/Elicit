# Use Case: Choose the Languages a Site Offers

## Overview

**Use Case ID:** UC-015  
**Use Case Name:** Choose the Languages a Site Offers  
**Primary Actor:** Deployment Operator  
**Goal:** Decide which of the languages the release carries this site offers, so that people see the site in a language they read and the operator supplies no translation of their own.  
**Status:** Open  
**Requirements:** [FR-026, FR-027, FR-031](../requirements.md)

## Preconditions

- The applications are installed by one of the two paths (UC-007 or UC-008) and the operator knows how a setting reaches them (UC-011).
- The operator knows which languages the site is to offer, and for which of the three applications.

## Main Success Scenario

1. The operator reads the translations chapter, which states that every image already carries the applications' own texts in every language the release supports — English, Latin American Spanish and Arabic — and that there is nothing to supply or lay out.
2. The system states that a released image renders the translation that version was built and tested with: languages are curated and arrive in a release, so a site can neither add one nor change one, only choose among them (UC-016).
3. The operator sets `i18n.bundled.locales` on each application to the languages this site is to offer — the survey application, the console, the authoring tool and the authoring preview each taking their own value, since a site may run them for different audiences.
4. The system states that the language selector in the header appears only once more than one language is offered, so a site left at `en` never shows one.
5. The system states what a withheld language does: it stays inside the image but is unreachable — absent from the selector, refused in a `?lang=` link, and not served for survey content either.
6. The system gives the resolution a reader actually sees: the exact language tag, then the language alone, then English. A text missing from every language renders as `!key!` and is logged once rather than rendering blank.
7. The system states plainly what this reaches and what it does not: it is the applications' own chrome, while survey content held in the database — question and answer text, answer options, step and section names — is translated by a second mechanism, written in Author against a particular survey and delivered inside that survey's definition file. Offering a language here does not translate a survey's questions, and publishing a survey in a language does not reach a site that does not offer that language for its own chrome; both are needed. Message templates and the body of an external report service are outside both and appear as authored.
8. The operator restarts each application and confirms that the selector lists the expected languages.

## Alternative Flows

### A1: A language is offered by one application and not another

**Trigger:** The site offers a language to respondents but not to its own staff (step 3)  
**Flow:**

1. The manual states that each application has its own setting, so a language named for the survey application alone reaches respondents and neither the console nor the authoring tool.
2. The manual states that this is a legitimate arrangement — a site whose respondents and whose staff read different languages — and not a misconfiguration.
3. Use case continues at step 4.

### A2: The selector does not list an expected language

**Trigger:** The selector offers fewer languages than the operator intended after the restart (step 8)  
**Flow:**

1. The manual gives the causes actually seen: a language missing from `i18n.bundled.locales`, a tag written in the wrong form (`es_419` rather than `es-419`), and a setting applied to one service but not the others.
2. The manual states that none of these produces an error, because a language a site does not name is indistinguishable from one it does not want.
3. Use case continues at step 3.

### A3: The site wants a language the release does not carry

**Trigger:** The site needs a language that is not in the image (step 1)  
**Flow:**

1. The manual states that this is a request to ElicitSoftware rather than a configuration change, and that the language arrives in a later release (UC-016).
2. The manual states that there is no file an operator can add in the meantime: the applications read their texts from inside the image, and nothing on the host is consulted.
3. Use case ends.

### A4: A shipped language does not suit the site's displays

**Trigger:** A right-to-left language reads too small, or a language needs a direction the release did not declare (step 8)  
**Flow:**

1. The manual states that `i18n.font-scale.<tag>` and `i18n.direction.<tag>` override what the release declares for one language, and that Arabic already ships right-to-left at a scale of 1.15.
2. The manual states that the scale multiplies the whole page for that language rather than only its letters, and that a value outside the accepted range is refused rather than applied.
3. Use case continues at step 8.

### A5: The site installs without containers

**Trigger:** The applications run as JVM services rather than containers (step 3)  
**Flow:**

1. The manual states that `i18n.bundled.locales` is an ordinary configuration setting reached by any Quarkus configuration source, so nothing about this differs outside Docker.
2. Use case continues at step 4.

## Postconditions

### Success Postconditions

- Each application offers exactly the languages the site named, resolves each text from the language a reader chose and falls back to English for anything that language does not carry.

### Failure Postconditions

- Each application runs in English alone and serves pages normally. A setting naming no usable language is not an error and does not prevent an application from starting.

## Business Rules

### BR-001: English is always available and is the last fallback

Every image carries the complete English texts and offers English whatever else the site offers. Any text a selected language does not carry is rendered in English rather than left blank, so a page is never partly empty.

### BR-002: A site chooses among the release's languages; it does not supply them

The set a site can offer is fixed by the version it runs. Narrowing it is a configuration choice; widening it beyond what the release carries is not possible, which is what keeps a translation and the code that renders it at the same version.

### BR-003: Each application is chosen for separately

An application offers what its own setting names. Making a language available across the site therefore means naming it for each application the site runs.

### BR-004: This mechanism translates chrome; survey content is translated elsewhere

Only the applications' own chrome — navigation, buttons, labels, notifications, validation messages, and document headers and footers — comes from the image. Survey content is translated too, but not in the image: those translations are written in Author against one survey and arrive at a site inside its definition file (Survey V019). Message templates and the body of an external report service are outside both mechanisms and appear as authored (FR-031).

### BR-005: Translation files are read server-side only

The translation files sit at the root of each application's classpath rather than among its web resources, so none of them is reachable over HTTP. A request for one is answered by the application's own page, not the file.

### BR-006: The language a reader sees is chosen per session

A `?lang=<tag>` parameter on a route wins, then the language remembered in the browser session, then the browser's own preference negotiated against the offered languages, then English. The choice lives in the session only; it is never stored against a respondent or a user account, and it never crosses sessions (Admin UC-026, Author UC-040).

### BR-007: A content language needs the site to offer that language

A respondent is offered a survey's content in a language only when that language is both published for the survey and offered by the site for the application's own texts. A site that withholds a language therefore holds the survey's translations of it without ever serving them, which is what lets one definition file suit every site (Survey UC-009 BR-009, Author UC-044 BR-002).
