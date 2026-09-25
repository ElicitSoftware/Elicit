# Use Case: Look Up a Setting

## Overview

**Use Case ID:** UC-012  
**Use Case Name:** Look Up a Setting  
**Primary Actor:** Deployment Operator  
**Goal:** Find any one setting an installed module reads — what it does, what it defaults to, and whether the site should change it — without reading a module's source or comparing three properties files.  
**Status:** Open  
**Requirements:** [FR-016, FR-017, FR-018](../requirements.md)

## Preconditions

- The operator has the manual (UC-001).
- The operator has a setting in hand, either by name or by the behaviour it governs.

## Main Success Scenario

1. The operator meets a setting — in the compose file, in a module's own properties file, in a log line, or in another chapter of the manual — and turns to the configuration reference.
2. The system states how the reference is arranged: the settings common to all three applications first, then one section per application for that application's own settings, then the framework settings the images actually set, then a pointer to the framework's own configuration index for everything else.
3. The operator reads the common section, which covers the brand directory, the translations directory and their local counterparts, and the health, metrics and tracing endpoints every module exposes.
4. The operator turns to the section for the application in hand and finds the setting with its default value, a sentence on what it does, and its marking as one the site must set, may set, or should leave alone (FR-019).
5. The system states that a default printed in the reference is the value the module ships with — the one in its properties file, or the one the code declares where no properties file sets it — and that the correspondence is checked when the manual is built.
6. The operator, for a setting that is not Elicit's own, reads the framework section, which gives the datasource, migration, identity, HTTP, mail and telemetry settings the images set and the values they set them to.
7. The operator, for a setting neither section lists, follows the reference's pointer to the framework's configuration index.
8. The operator applies the change through the configuration mechanism, choosing a source and writing the value there (UC-011).

## Alternative Flows

### A1: The setting is in neither of the manual's sections

**Trigger:** The operator wants to change a framework setting the images leave at its own default (step 7)  
**Flow:**

1. The manual states that it lists the framework settings the images set, not the framework's whole catalogue, that every other setting of the framework is nonetheless available to a site, and that it is left at the framework's own default.
2. The manual gives the address of the framework's configuration index and stops there (FR-018).
3. Use case continues at step 8.

### A2: The operator has the environment-variable form of the name

**Trigger:** The operator is reading a container's environment rather than a properties file (step 4)  
**Flow:**

1. The manual gives the mapping rule in both directions and states that reading it backwards is not always unambiguous, because two setting names differing only in punctuation share one variable name.
2. The manual states that the reference is indexed by the dotted name, which is the form to search for, and that the shipped compose file writes most of its entries in that form already.
3. Use case continues at step 4.

### A3: The same name appears for more than one application

**Trigger:** The setting is one that two or three of the applications read (step 4)  
**Flow:**

1. The manual states that a shared name may carry a different default in each application — the translations sub-directory each one reads is the plainest case — and that the per-application section gives the value for the application in hand.
2. The manual states that a setting listed only in one application's section is read only by that application, whatever a site may have set elsewhere.
3. Use case continues at step 4.

### A4: The setting was found in a shipped properties file under a profile prefix

**Trigger:** The operator is reading a module's source file rather than the manual (step 1)  
**Flow:**

1. The manual states that a profile-prefixed line applies only while that profile is active, that the prefixed lines in the shipped files serve development and the module test suites, and that the reference prints the value a deployment actually gets (UC-011 BR-003).
2. Use case continues at step 2.

### A5: The operator needs the value the application is running with

**Trigger:** The documented default does not match what the application appears to be doing (step 5)  
**Flow:**

1. The manual points at the console's System section, which reports the resolved database, branding, mail and identity configuration of a running application (FR-033).
2. The manual states that the reference gives the shipped default and the running application gives the effective value, and that a difference between them is the site's own configuration doing its work.
3. Use case continues at step 8.

### A6: A release adds a setting the reference has not caught up with

**Trigger:** A setting read by a module does not appear in the reference (step 4)  
**Flow:**

1. The manual states that its build checks every setting the three applications read against the reference and fails when one is missing, so that such a gap is a build failure rather than a silent omission (NFR-004).
2. Use case continues at step 7.

## Postconditions

### Success Postconditions

- The operator can state what the setting does, what it defaults to, whether the site should change it, and where a change is written.

### Failure Postconditions

- None. Reading changes nothing. A setting the reference does not cover leaves the operator at the framework's index, which is where the manual intends to leave them (A1).

## Business Rules

### BR-001: Common settings once, application settings per application

A setting all three applications read appears once, in the common section. A setting one application reads appears in that application's section. No setting appears twice with two descriptions.

### BR-002: Elicit settings in full, framework settings where set

Every setting Elicit's own code reads is listed. A framework setting is listed only where an image sets it, because that is what a site is overriding when it changes one (FR-017).

### BR-003: A printed default is a shipped value

Every default in the reference is copied from the module's properties file or from the default its code declares. No default is restated from prose, and none is inferred (NFR-005).

### BR-004: The manual does not restate another project's documentation

Settings the manual does not list are reached through one pointer to the framework's configuration index. The reference stays a manual rather than becoming a copy (FR-018).

### BR-005: Coverage is checked, not asserted

The claim that the reference covers every setting the three applications read is verified by the manual's own build, so that the claim cannot quietly stop being true between releases (NFR-004).
