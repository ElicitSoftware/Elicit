# Use Case: Configure an Application

## Overview

**Use Case ID:** UC-011  
**Use Case Name:** Configure an Application  
**Primary Actor:** Deployment Operator  
**Secondary Actor:** SMTP Relay  
**Goal:** Supply the survey application, the console or the authoring tool with the site's own configuration, by one mechanism that is the same for a container and for a JVM service, so that each module runs against the site's resources and no internal default is changed by accident.  
**Status:** Open  
**Requirements:** [FR-019, FR-020](../requirements.md)

## Preconditions

- The operator has decided which modules the site will run (UC-003).
- The database roles exist with the site's passwords (UC-005) and the identity provider's clients exist with their secrets (UC-006).
- The operator has the address of a mail relay if the site runs the console.

## Main Success Scenario

1. The operator reads the configuration chapter, which opens with the one mechanism all three applications share, so that the rules are learnt once and then applied per module.
2. The system gives the ordered list of sources a value may be read from — system properties, environment variables, a `.env` file, a properties file in a `config` directory beside the runnable artifact, and the properties file built into the artifact — and states that a source earlier in that list overrides every source after it.
3. The system gives the rule that turns a setting's dotted name into an environment variable name, and states that in the container path a setting may equally be written under its dotted name as an environment entry, which is how the shipped compose file writes most of them.
4. The system explains the profile prefix: a line whose name begins with a profile marker is read only while that profile is active, the prefixed lines in the shipped files serve development and the module test suites, and the chapter states which profile a deployed application actually runs under so that the operator knows which lines apply to a site.
5. The operator works through the settings marked as ones the site must set: each module's two datasources, the identity provider's address, client identifier and secret, the mail relay, the brand and translations directories, and the addresses by which the modules reach one another.
6. The operator reads the settings marked as ones the site may set, changes those the site needs, and leaves untouched the settings marked as internal defaults.
7. The operator writes the chosen values into one source — environment entries in the compose file in the container path, or a properties file or the service manager's environment in the module-by-module path.
8. The operator starts or restarts the module and confirms from its readiness endpoint and from the console's system screens that it reached the resources intended (UC-018).

## Alternative Flows

### A1: The same setting is supplied in two places

**Trigger:** A value the operator set does not take effect (step 8)  
**Flow:**

1. The manual states that a setting given in more than one source is resolved by the order of step 2, and that an environment variable therefore overrides a properties file, which is the case met most often.
2. The manual points at the console's system screens, which report the configuration an application is actually running with, so that the effective value is read from the application rather than inferred from the files.
3. Use case continues at step 7.

### A2: A setting name does not map cleanly to an environment variable

**Trigger:** The setting's name carries a quoted segment, or differs from another name only in punctuation (step 3)  
**Flow:**

1. The manual states that the mapping upper-cases the name and replaces every character that is neither a letter nor a digit with an underscore, so that a quoted segment becomes a pair of underscores on each side and two different setting names can collapse onto one variable name.
2. The manual states that such a setting is supplied as a property instead, or as an environment entry under its dotted name where the runtime accepts one.
3. Use case continues at step 7.

### A3: The site keeps its configuration outside the image

**Trigger:** Site policy forbids credentials in a file kept under version control (step 7)  
**Flow:**

1. The manual states that the properties file in a `config` directory beside the runnable artifact, and the environment the service manager supplies, are both read without rebuilding or editing an image, and gives each one's place in the source order.
2. Use case continues at step 7.

### A4: The setting carries a secret

**Trigger:** The setting being set is a database password, a client secret or a mail credential (step 5)  
**Flow:**

1. The manual identifies the setting in the secrets section, with the development value it ships with and where that value is set today (UC-013).
2. Use case continues at step 5.

### A5: The configuration is for the authoring tool's preview instance

**Trigger:** The site runs the authoring tool (step 5)  
**Flow:**

1. The manual states that the preview instance is the survey application configured differently, not a further module: both its datasources address the authoring database, it registers any access code that is typed in so that an author can walk a draft, and it does not build or update the reporting schema.
2. The manual states that all three differences are deliberate and that none of them belongs on a site's survey application.
3. Use case continues at step 5.

### A6: A setting has no effect on a running deployment

**Trigger:** A framework setting the operator changed makes no difference after a restart (step 8)  
**Flow:**

1. The manual states that some framework settings are fixed when an application is built and cannot be changed by a deployment, that the reference marks each of them among the settings to leave alone, and that changing one in a running site changes nothing.
2. Use case continues at step 6.

## Postconditions

### Success Postconditions

- Each module the site runs reads its addresses, credentials and directories from a source the site controls, and no setting the reference marks as an internal default has been changed.

### Failure Postconditions

- A module either runs with a value it was not meant to have, which the console's system screens report, or fails to start and names the setting in its log. A module that fails to start writes nothing to the database.

## Business Rules

### BR-001: One mechanism for both installation paths

A container and a JVM service are configured by the same rules, from the same sources, in the same order. The chapter describes the mechanism once; the installation chapters differ only in which source they find it convenient to use (UC-008 BR-003).

### BR-002: The earlier source wins

A value from a source earlier in the order replaces the same setting from every later source, including the one built into the artifact. A site never has to change a shipped file in order to change a shipped value.

### BR-003: A profile-prefixed line is not a setting to copy

A line carrying a profile marker belongs to development or to a module's test suite. Copying one into a deployment's configuration without the marker applies a value that was never meant for a site.

### BR-004: Three markings, and only three

Every setting in the reference is marked as one the site must set, may set, or should leave alone. A setting with no marking is an omission, not a fourth category.

### BR-005: The preview instance is deliberately unlike a site's survey application

The authoring tool's preview differs from a site's survey application in exactly the settings the manual names, and it differs on purpose. A site that copies its configuration gets a survey application that admits any access code and keeps no reporting data.

### BR-006: No setting a site is expected to change requires a new image

Every setting marked as one the site must or may set can be supplied to an existing image. The settings that are fixed when an application is built are all marked as ones to leave alone (A6).
