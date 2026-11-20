# Use Case: Choose the Languages a Site Offers

## Overview

**Use Case ID:** UC-015  
**Use Case Name:** Choose the Languages a Site Offers  
**Primary Actor:** Deployment Operator  
**Goal:** Supply the languages this site offers by mounting translation files, and narrow them where one application should offer fewer, so that people see the site in a language they read.  
**Status:** Open  
**Requirements:** [FR-026, FR-027, FR-031](../requirements.md)

## Preconditions

- The applications are installed by one of the two paths (UC-007 or UC-008) and the operator knows how a setting reaches them (UC-011).
- The operator knows which languages the site is to offer, and for which of the two applications.

## Main Success Scenario

1. The operator reads the translations chapter, which states that each image carries the applications' own texts in English only. Every other language is a translation file the site supplies in a mounted directory.
2. The system states the layout of that directory: one sub-directory per application (`survey/`, `admin/`), each holding `translations_<tag>.properties` files. The tag is written with underscores in the file name (`translations_es_419.properties`). An optional `i18n-config.json` at the root declares text direction and size per language. Each application reads `<i18n.file.system.path>/<i18n.app.name>/`, which is `/i18n/survey/` and `/i18n/admin/` by default.
3. The system states that ElicitSoftware maintains a curated set beside each release, the repository's `elicit-i18n/`, holding Latin American Spanish and Arabic for both applications. The compose file mounts it read-only at `/opt/i18n` and sets `i18n.file.system.path` to match. A site may mount that set, only part of it, or files of its own (UC-016, UC-017).
4. The operator mounts the directory on each application, or leaves it unmounted for an English-only site.
5. Where an application should offer fewer languages than are mounted, the operator sets `i18n.offered.locales` on it to the tags it is to offer. The survey application and the console each take their own value, since a site may run them for different audiences.
6. The system states which languages an application offers: English, plus every language mounted in its own sub-directory, narrowed to the tags `i18n.offered.locales` lists when that is set. English is always offered.
7. The system states that the language selector in the header appears only once more than one language is offered. An application with nothing mounted never shows one.
8. The system states what a language that is not offered does: it is absent from the selector, refused in a `?lang=` link, and not served for survey content either.
9. The system gives the order in which a text is resolved: the exact language tag, then the language alone, then English. A mounted file may hold only some texts, and each missing one is shown in English. A text missing from every language renders as `!key!` and is logged once rather than rendering blank.
10. The system states plainly what this reaches and what it does not. It covers the applications' own chrome. Survey content held in the database is translated by a second mechanism: written in Author against a particular survey and delivered inside that survey's definition file. That content covers:
    - question and answer text;
    - answer options;
    - step and section names.

    Both are needed. Offering a language here does not translate a survey's questions, and a survey published in a language is not shown in it at a site that does not offer that language for its own chrome. Message templates and the body of an external report service are outside both and appear as authored.
11. The operator restarts each application and confirms that the selector lists the expected languages.

## Alternative Flows

### A1: A language is offered by one application and not another

**Trigger:** The site offers a language to respondents but not to its own staff (step 5)  
**Flow:**

1. The manual states that each application reads its own sub-directory and its own setting. A language mounted or named for the survey application alone reaches respondents and not the console.
2. The manual states that this is a legitimate arrangement — a site whose respondents and whose staff read different languages — and not a misconfiguration.
3. Use case continues at step 6.

### A2: The selector does not list an expected language

**Trigger:** The selector offers fewer languages than the operator intended after the restart (step 11)  
**Flow:**

1. The manual gives the causes actually seen:
   - the directory is not mounted, or `i18n.file.system.path` does not name the mount point;
   - the file sits in the wrong application's sub-directory;
   - the file name is in the wrong form, for example `translations_es-419.properties` instead of `translations_es_419.properties`;
   - the language is missing from `i18n.offered.locales`, or a tag there is written in the wrong form (`es_419` rather than `es-419`).
2. The manual states that none of these stops an application from starting. An application logs the directory it found nothing in, and each tag `i18n.offered.locales` lists that has no mounted file.
3. Use case continues at step 4.

### A3: The site wants a language the curated set does not include

**Trigger:** The site needs a language that is not in `elicit-i18n/` (step 3)  
**Flow:**

1. The manual states that the site may commission the translation itself (UC-017) and mount the returned file, or ask ElicitSoftware to add it to the curated set (UC-016).
2. Use case continues at step 4.

### A4: A language does not suit the site's displays

**Trigger:** A right-to-left language reads too small, or a language needs a direction nothing declares (step 11)  
**Flow:**

1. The manual states the three places a language's direction and size are declared, each overriding the one before:
   - the image;
   - `i18n-config.json` at the root of the mount;
   - the `i18n.direction.<tag>` and `i18n.font-scale.<tag>` properties, for one application.

   The curated `i18n-config.json` sets Arabic right to left at a scale of 1.15.
2. The manual states that the scale multiplies the whole page for that language rather than only its letters, and that a value outside the accepted range is refused rather than applied.
3. Use case continues at step 11.

## Postconditions

### Success Postconditions

- Each application offers the languages mounted for it, narrowed to those the site named. Each text resolves from the language a reader chose, and anything that language does not carry falls back to English.

### Failure Postconditions

- Each application runs in English alone and serves pages normally. An empty or missing directory, or a setting naming no usable language, is not an error and does not prevent an application from starting.

## Business Rules

### BR-001: English is always available and is the last fallback

Every image carries the complete English texts and offers English whatever else the site offers. Any text a selected language does not carry is rendered in English rather than left blank, so a page is never partly empty.

### BR-002: A site supplies its languages; a mounted file overlays English text by text

The image carries English only. A language is offered because a file for it is mounted, and that file may hold every text or only some. The texts it holds replace the English ones and the rest stay in English, so a site can also correct one wording without a release. ElicitSoftware's curated set is the wording a release was tested with. A site that mounts different files owns what they say.

### BR-003: Each application is chosen for separately

An application offers what is mounted in its own sub-directory, narrowed by its own setting. Making a language available across the site therefore means mounting it, and naming it where the setting is used, for each application the site runs.

### BR-004: This mechanism translates chrome; survey content is translated elsewhere

This mechanism covers only the applications' own chrome:

- navigation and buttons;
- labels and notifications;
- validation messages;
- document headers and footers.

Survey content is translated too, but not here: those translations are written in Author against one survey and arrive at a site inside its definition file (Survey V019). Message templates and the body of an external report service are outside both mechanisms and appear as authored (FR-031).

### BR-005: Translation files are read server-side only

The applications read the mounted files from the file system. They are not among any application's web resources, so none of them is reachable over HTTP.

### BR-006: The language a reader sees is chosen per session

The language is chosen in this order:

1. A `?lang=<tag>` parameter on a route.
2. The language remembered in the browser session.
3. The browser's own preference, negotiated against the offered languages.
4. English.

The choice lives in the session only. It is never stored against a respondent or a user account, and it never crosses sessions (Admin UC-026).

### BR-007: A content language needs the site to offer that language

A respondent is offered a survey's content in a language only when that language is both published for the survey and offered by the site for the application's own texts. A site that does not offer a language therefore holds the survey's translations of it without ever serving them, which is what lets one definition file suit every site (Survey UC-009 BR-009).
