# Use Case: Commission a Translation

## Overview

**Use Case ID:** UC-017  
**Use Case Name:** Commission a Translation  
**Primary Actor:** Deployment Operator  
**Secondary Actor:** Translator (person or AI agent)  
**Goal:** Hand a translator everything needed to produce a language file for one application, and receive back a file the deployment can mount without editing it.  
**Status:** Open  
**Requirements:** [FR-030](../requirements.md)

## Preconditions

- The translations directory is mounted and the operator knows which sub-directory the language will be placed in (UC-015).
- The site has decided which language it needs and which applications it needs it for.

## Main Success Scenario

1. The operator decides which applications the language is for, the manual having stated that the survey application, the console and the authoring tool are translated separately and each needs its own file.
2. The operator takes that application's translation request document, one per application, which the repository carries beside the application's translation files and which is generated from the application's English texts and a per-key context file rather than written by hand.
3. The system states what the document already contains, so that the operator adds nothing to it: what the application is for and who uses it, a glossary of the terms that must stay consistent or stay untranslated, the rules for placeholders and for embedded markup, and every key with its English text, where it appears, its maximum length where it has one, and its flags.
4. The system gives the document's own return instruction: exactly one `translations_<tag>.properties` file, UTF-8 encoded, carrying the same keys in the same order as the English texts at the end of the document, one `key=translation` per line.
5. The operator chooses the language tag, writing it with a hyphen — `es-419`, `pt-BR` — and tells the translator that the returned file name carries the same tag with underscores.
6. The operator tells the translator what the document deliberately leaves out: the organisation name and the brand description, which are translated in the brand directory instead (UC-014 BR-005), and the survey content held in the database, which this mechanism does not translate at all (UC-015 BR-004).
7. The translator returns one file, and the operator checks it against the document's own rules — the keys unchanged, the placeholders matching, no lone apostrophe in a text that takes parameters, and the maximum lengths respected.
8. The operator deploys the file (UC-016) and reviews the rendered pages, paying particular attention to the texts the document gave a maximum length and, for a right-to-left language, to the mirrored layout.
9. The operator keeps the returned file with the site's configuration, so that it survives a reinstallation or an upgrade (UC-011).

## Alternative Flows

### A1: The document does not match the running release

**Trigger:** The site runs a release whose texts differ from the document the operator has (step 2)  
**Flow:**

1. The manual states that the document is generated from one release's English texts, and that a translation commissioned from a stale document will be missing keys the running release added.
2. The manual states the remedy — take the document from the repository at the release the site runs — and that the consequence of getting it wrong is not a failure but English appearing where the new texts should be.
3. Use case continues at step 3.

### A2: The translator is an AI agent

**Trigger:** The site has no human translator for the language (step 2)  
**Flow:**

1. The manual states that the document is written to be handed to an AI agent unchanged, which is why it carries the audience, the register and the glossary rather than assuming them.
2. The manual states that the checks in step 7 matter more in this case, not less, and that an agent's output is reviewed by someone who reads the language before it reaches respondents.
3. Use case continues at step 7.

### A3: Only part of the application is translated

**Trigger:** The translator returns a file covering some of the keys (step 7)  
**Flow:**

1. The manual states that a partial file is a valid deployment artefact: the keys it carries are used and the rest fall back to English (UC-015 BR-002).
2. The manual states the visible result — pages mixing the two languages — so that the operator decides deliberately whether to deploy it or to complete it first.
3. Use case continues at step 8.

### A4: The same language is wanted for more than one application

**Trigger:** The site offers the language to respondents and to its own staff (step 1)  
**Flow:**

1. The manual states that this is three documents and three returned files, one per application, because the key sets do not overlap.
2. The manual states that the glossary terms shared between the documents — the product name, the access code, the subject and respondent distinction — should be translated the same way across all of them, and that this is the operator's instruction to give, since nothing checks it.
3. Use case continues at step 2.

### A5: The language is written right to left

**Trigger:** The commissioned language is Arabic, Hebrew, Persian, Urdu or another right-to-left language (step 5)  
**Flow:**

1. The manual states that the translator writes the text naturally and adds no markers: direction is a property of the deployment, declared where it is not already the default (UC-016 step 5), not a property of the file.
2. Use case continues at step 6.

## Postconditions

### Success Postconditions

- The site holds one translation file per application for the language, each checked against the request document's rules and ready to be placed in the mount (UC-016).

### Failure Postconditions

- No file is deployed and the site offers the languages it offered before. A translation that was commissioned and not returned changes nothing.

## Business Rules

### BR-001: One document, one application

Each application generates its own request document from its own texts. There is no combined document, because the applications share no keys and are read by different people.

### BR-002: The document is self-contained

Everything a translator needs — purpose, audience, register, glossary, placeholder and markup rules, every key with its context and its English text, and the return format — is in the document. A translator needs no access to the running site, to the source or to this manual.

### BR-003: Placeholders are preserved and apostrophes depend on them

A text's placeholders must be exactly those of its English original; they may be moved within the sentence but never removed, renamed or exchanged. A text that takes parameters is a formatting pattern, so an apostrophe in it must be doubled; a text that takes none is not, so an apostrophe in it is written normally.

### BR-004: Keys are not translated

The keys are the application's own identifiers. A translation adds none, removes none, reorders none and translates none, which is what lets a returned file be compared with the English one mechanically.

### BR-005: Brand text is not in the document

The organisation name and the brand description belong to the mounted brand and are translated there. Putting them in an application's translation file would place the same text in two places and make the brand's own value unreachable.

### BR-006: A returned file is deployment data

The commissioned file belongs to the site's mounted translations directory, not to an application image. A site that wishes to carry a language in a build of its own places it in the local tier instead (UC-015 A4), and the mount still overrides it.
