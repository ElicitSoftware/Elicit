# Use Case: Mount an External Brand

## Overview

**Use Case ID:** UC-014  
**Use Case Name:** Mount an External Brand  
**Primary Actor:** Deployment Operator  
**Goal:** Give the survey application, the console and the authoring tool the site's own colours, typefaces, logos and organisation name by mounting a brand directory, so that the site is branded without rebuilding any image.  
**Status:** Open  
**Requirements:** [FR-022, FR-023, FR-024, FR-025](../requirements.md)

## Preconditions

- The applications are installed by one of the two paths (UC-007 or UC-008) and the operator knows how a setting reaches them (UC-011).
- The site has its colour values, its typeface stack and its logo, icon and favicon files.

## Main Success Scenario

1. The operator reads the branding chapter, which states that every application carries a complete brand inside its image and that mounting a brand replaces files of that default rather than the whole of it.
2. The system gives the directory layout a brand must follow: `brand-config.json` and `brand-info.json` at the root, `colors/brand-colors.css`, `typography/brand-typography.css` with its `typography.json` companion, `images/` holding the horizontal logo, the icon and the favicon, and `theme.css`, which imports the colour and typography stylesheets and is the file an application inlines into the page head.
3. The system states that a brand populates `--brand-*` custom properties only: each application's own stylesheet maps the Lumo theme tokens onto them, so a brand never sets a `--lumo-*` value itself, and a brand that defines only `--brand-*` values needs no knowledge of the applications.
4. The operator assembles the site's brand directory, starting from the repository's default brand and changing only the files that differ, and names the brand in `brand-config.json` along with the logo, icon and favicon file names it expects to find under `images/`.
5. The operator points each application at the directory with `brand.file.system.path` — which defaults to `/brand`, and which the compose file sets to `/opt/brand` with a read-only volume — and gives the survey application, the console, the authoring tool and the authoring preview the same mount.
6. The operator restarts each application, because the resolved brand is cached for the life of the service.
7. The operator opens Branding in the console's System section (Admin UC-023) and in the authoring tool's (Author UC-036), which reports the configured path, whether that directory exists, which metadata files are present in it, and, for each asset, whether it resolved from the mount, from the local directory, from the embedded default or from nowhere.
8. The operator, if the site offers more than one language, translates the brand's own texts in the brand itself: an optional `localized` block keyed by language tag in `brand-config.json` carries `name` and `organization`, and the matching block in `brand-info.json` carries `description`.
9. The operator proceeds to the translations directory (UC-015).

## Alternative Flows

### A1: Only part of the brand is replaced

**Trigger:** The site wants its own colours but keeps the supplied typography, logo and favicon (step 4)  
**Flow:**

1. The manual states that fallback is per file: a mounted directory holding only `colors/brand-colors.css` is valid, and every other asset resolves to the embedded default.
2. The manual names the repository's partial brand directory as the worked example of that case, and warns that a partial mount is the failure that is otherwise invisible, because the pages render with the wrong typeface rather than failing.
3. Use case continues at step 5.

### A2: No brand resolves

**Trigger:** The applications still show the default brand after the restart (step 7)  
**Flow:**

1. The branding screen reports every asset as embedded, which means the configured path does not exist inside the container or holds no brand files.
2. The manual states the two causes actually seen — a path that does not match the volume's target, and a container that was restarted rather than recreated after the volume was changed — and gives the check for each.
3. Use case continues at step 5.

### A3: The applications are given different brands

**Trigger:** The site brands the subject-facing survey application differently from its internal console (step 5)  
**Flow:**

1. The manual states that `brand.file.system.path` is set per application, so each service may mount a different directory, and that nothing in the platform requires the three to agree.
2. The manual states that the authoring preview instance should be given the same brand as the authoring tool, so that an author previewing a draft sees what a respondent will see.
3. Use case continues at step 6.

### A4: The brand is changed on a running site

**Trigger:** An asset in the mounted directory is corrected after the applications have started (step 6)  
**Flow:**

1. The manual states that the resolved brand is cached for the life of the service, so editing a file in the mount changes nothing by itself.
2. The manual gives the two ways to pick the change up: Reload brand on the branding screen, which discards the cache and re-resolves, or a restart of the application.
3. Use case continues at step 7.

### A5: The site installs without containers

**Trigger:** The applications run as JVM services rather than containers (step 5)  
**Flow:**

1. The manual states that `brand.file.system.path` is an ordinary configuration setting reached by any Quarkus configuration source, and that it names a directory on the host with the same layout.
2. The manual states that `brand.local.path`, which defaults to `brand`, names a directory relative to the service's working directory and is consulted between the mount and the embedded default.
3. Use case continues at step 6.

## Postconditions

### Success Postconditions

- Each application resolves the site's brand, every asset the mount supplies comes from it, every asset it omits comes from the embedded default, and the branding screen reports the source of each one.

### Failure Postconditions

- The applications run on their embedded default brand and serve pages normally. Nothing fails; the only report of the failure is the branding screen.

## Business Rules

### BR-001: The default brand is always present

Every application embeds a complete brand and can never be without one. A mount is an override, so a deployment that mounts nothing is a supported configuration rather than an unbranded one.

### BR-002: Fallback is per file

Each expected asset is resolved on its own, in the order mount, local directory, embedded default. A mounted directory is never rejected for being incomplete, and a missing file never disables the assets beside it.

### BR-003: A brand supplies `--brand-*`, never `--lumo-*`

The brand defines only its own custom properties. Each application's stylesheet maps the Lumo tokens onto them, which is what lets one brand directory serve the survey application, the console and the authoring tool unchanged.

### BR-004: The base name is the brand key

The base `name` in `brand-config.json` derives the technical brand key the applications use to identify the brand. A `localized` block changes only what is displayed; the base `name` must stay as it is, whatever language a page is rendered in.

### BR-005: Brand text is translated in the brand

The organisation name in the header and the brand description in the page metadata come from the brand directory, so they are translated there and never in an application's translation files (UC-015). A localised value resolves by exact tag, then by language alone, then by the base value.

### BR-006: The resolved brand is cached

An application resolves its brand once and holds it. A change to a mounted file takes effect on an explicit reload from the branding screen or on the next restart, and at no other time.
