# Use Case: Add, Replace or Remove a Language

## Overview

**Use Case ID:** UC-016  
**Use Case Name:** Add, Replace or Remove a Language  
**Primary Actor:** Deployment Operator  
**Goal:** Make a language available to an application, replace the language's texts, or withdraw it, by placing or removing one file in the mounted translations directory and restarting the application.  
**Status:** Open  
**Requirements:** [FR-028, FR-029, C-012, C-013](../requirements.md)

## Preconditions

- The translations directory is mounted and each application reads its own sub-directory of it (UC-015).
- The operator has a completed translation file for the language, or is withdrawing one already in place (UC-017).

## Main Success Scenario

1. The operator reads the language chapter, which states that adding, replacing and removing a language is a file operation performed on the server today, and that the Languages screens specified for the console and the authoring tool are not built (Admin UC-027, Author UC-041).
2. The system states that a language is added to one application at a time: giving the console a language does not give it to the survey application, and the authoring preview takes whatever the survey application's sub-directory holds.
3. The operator checks the file before it is deployed — that it is a UTF-8 properties file, that every key in it exists in the application's English texts, that each text carries exactly the placeholders its English original carries, and that no parameterised text contains a lone apostrophe — using the rules the console's specification states (Admin UC-027 BR-003, BR-004 and BR-005).
4. The operator places the file in the application's sub-directory of the mount as `translations_<tag>.properties`, writing the tag with underscores in the file name — `translations_es_419.properties` — while the tag itself is written with hyphens everywhere else.
5. The operator checks the language's layout direction: Arabic, Hebrew, Persian, Urdu, Pashto, Sindhi, Uyghur, Yiddish, Dhivehi and Kurdish (Sorani) are right-to-left without being declared, and any other right-to-left language is declared in `i18n-config.json` at the root of the mount, which may also force a language the other way.
6. The operator restarts each application whose sub-directory was changed, because the directory is read on startup and the languages an application offers are held for the life of the service.
7. The operator confirms the result: the language appears in the header's selector, the pages render in it, and a right-to-left language mirrors the layout rather than only changing the words.
8. The operator records the language and its file with the site's configuration, so that a later reinstallation or upgrade recreates the same mount (UC-011).
9. The operator proceeds to verification (UC-018).

## Alternative Flows

### A1: The language's texts are replaced

**Trigger:** A corrected or extended translation arrives for a language already deployed (step 4)  
**Flow:**

1. The manual states that the file is the unit of change: the returned file replaces the whole of the previous one for that tag and application, and there is no way to change one text on its own.
2. The operator overwrites the file and restarts the application, as in steps 6 and 7.
3. Use case continues at step 8.

### A2: A language is removed

**Trigger:** The site withdraws a language it previously offered (step 4)  
**Flow:**

1. The operator deletes the language's file from the application's sub-directory, and its entry from `i18n-config.json` if it has one.
2. The operator restarts the application; the language leaves the selector, and a session that was using it falls back to English on its next page.
3. The manual states that English is built into every image and can never be removed this way.
4. Use case ends.

### A3: Only a few texts are changed

**Trigger:** The site wants to reword part of an application without commissioning a language (step 3)  
**Flow:**

1. The manual states that a file carrying only the keys to change is a valid override: every other key keeps the value from the tier below, ending at English (UC-015 BR-002).
2. The manual states that this applies to English itself — a `translations.properties` on the mount rewords the shipped English texts by the same rule.
3. Use case continues at step 6.

### A4: The file is wrong and nothing reports it

**Trigger:** A deployed file carries a misspelled key, a missing placeholder or a lone apostrophe in a parameterised text (step 7)  
**Flow:**

1. The manual states that no application validates a mounted file: a key it does not recognise is simply never read, and a text whose placeholders do not match its English original is rendered as written or mangled by the formatter.
2. The manual states the symptoms this produces — an English text where a translated one was expected, and a text showing a brace-delimited placeholder or losing an apostrophe — and that a key defined in no tier renders as `!key!`.
3. The operator corrects the file and repeats from step 3, which is where the checks belong until a Languages screen performs them.
4. Use case continues at step 3.

### A5: The in-application Languages screens — not yet available

**Trigger:** The operator looks for the Languages screen described in the console's and the authoring tool's specifications (step 1)  
**Flow:**

1. The manual states that these screens are specified and not implemented: Admin UC-027 (Admin FR-034) for the console and the survey application, and Author UC-041 (Author FR-054) for the authoring tool. Neither application has a Languages address, and no procedure in this manual depends on one.
2. The manual states what they will change when they are built: an administrator will upload a translation file through the application, which will validate it, summarise it, record the direction and reload the translations, so that a language can be added without server access.
3. The manual states that they will need the translations directory to be mounted writable, which the compose file does not do today (C-013, Admin C-015, Author C-024), and that until then the procedure in this use case is the only one.
4. Use case continues at step 2.

## Postconditions

### Success Postconditions

- The application's sub-directory holds exactly the language files the site intends, direction is declared where it is not the default, and every restarted application offers those languages in its selector.

### Failure Postconditions

- The directory holds whatever the operator left in it. No application refuses to start over a translation file: a file it cannot use leaves the affected texts in English, and the site runs in a state the operator has to observe rather than be told about.

## Business Rules

### BR-001: The server-side file procedure is the operative one

Adding, replacing and removing a language is done on the server, by placing or deleting a file and restarting the application (C-012). The Languages screens specified in Admin UC-027 and Author UC-041 are both at Draft and neither module has a Languages view; this manual documents them as intent and gives no procedure for them (NFR-010).

### BR-002: A language is added per application

The console, the survey application and the authoring tool each read only their own sub-directory. There is no operation that adds a language to the site as a whole; there is the same operation performed once per application.

### BR-003: The file is the unit of change

A deployment changes a language by replacing its whole file. There is no partial update and no per-text edit, which is what makes a language's state on a site readable from the directory listing alone.

### BR-004: English is built in and cannot be removed

Every image carries the English texts and always offers English. A `translations.properties` on the mount overrides individual English texts; it cannot remove them, and it cannot take English out of the selector.

### BR-005: A restart is what makes a change take effect

The translations directory is read when an application starts, and the set of offered languages is held for the life of the service. Editing a file changes nothing until the application is restarted, and an operator who checks before restarting will see the previous state.

### BR-006: A read-only mount is sufficient today, and will not be

The operator writes to the directory on the host, not through an application, so the read-only mount in the compose file is no obstacle to this procedure. It is an obstacle to the specified screens, which write the file themselves; a site intending to use them will have to mount the directory writable (C-013).
