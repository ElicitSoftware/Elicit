# Use Case: Build the Installation Manual

## Overview

**Use Case ID:** UC-002  
**Use Case Name:** Build the Installation Manual  
**Primary Actor:** Release Engineer  
**Goal:** Typeset the installation manual from its LaTeX sources and stamp it with a platform version and build date, on any machine with a container runtime and no TeX installation.  
**Status:** Open  
**Requirements:** [FR-002, NFR-001, NFR-002, NFR-003, NFR-008, C-002, C-003](../requirements.md)

## Preconditions

- The umbrella repository is checked out.
- A container runtime is available and running.

## Main Success Scenario

1. The release engineer runs `docs/manual/build-manual.sh`, optionally giving the platform version and the build date, or runs `./buildDockerImages.sh` (target `Manual`), which checks the configuration reference (A4) and then runs the same script.
2. The system determines the version: the argument if given, otherwise the repository's current release tag, otherwise "unknown".
3. The system determines the build date: the argument if given, otherwise the current UTC time.
4. The system writes the stamp to a generated file that the brand package reads, so the same sources build by hand and in a release.
5. The system typesets the manual with LuaLaTeX through `latexmk` inside a TeX Live container, which supplies the IBM Plex typefaces and the LaTeX packages.
6. The system writes the PDF beside the sources and reports where it landed and how large it is.
7. The release engineer publishes the PDF with the release.

## Alternative Flows

### A1: No container runtime

**Trigger:** No container runtime is available (step 5)  
**Flow:**

1. The system says so, names the environment variable that skips the build, and exits non-zero.
2. Use case ends.

### A2: The build is skipped deliberately

**Trigger:** The release engineer sets the skip variable (step 1)  
**Flow:**

1. The system says the manual is not being built and exits zero, so the containing build is not failed.
2. Use case ends.

### A3: The sources do not typeset

**Trigger:** LaTeX reports an error (step 5)  
**Flow:**

1. The system halts on the first error with the file and line, and exits non-zero. No PDF is written.
2. The release engineer fixes the source.
3. Use case continues at step 1.

### A4: The configuration reference has drifted from the code

**Trigger:** `docs/manual/check-properties.sh` runs — as the first half of the `Manual` target of `./buildDockerImages.sh`, or run directly by the release engineer (step 1)  
**Flow:**

1. The system compares every configuration key read by Survey, Admin and Author against the keys documented in the manual, and every documented default against the value in the module's sources. A default cell that states no value ("per app") claims nothing and is not compared.
2. The system lists each key that is read but not documented, each key documented but no longer read, and each default that disagrees, and exits non-zero.
3. Within a build, the target fails there: nothing is typeset, and a manual that documents keys the code no longer reads is never produced. `SKIP_PROPERTY_CHECK=1` typesets without the check.
4. The release engineer corrects the manual or accepts the difference.
5. Use case continues at step 1.

### A5: The TeX Live image is not yet pulled

**Trigger:** The container image is absent locally (step 5)  
**Flow:**

1. The runtime pulls it, which dominates the first build's elapsed time.
2. Use case continues at step 5, and subsequent builds meet the typesetting time requirement (NFR-008).

## Postconditions

### Success Postconditions

- A PDF exists carrying the supplied version and build date on its title page and in every page footer.

### Failure Postconditions

- No PDF is written, or a previously built PDF is left untouched. The generated stamp file may exist and is regenerated on the next build; it is not committed.

## Business Rules

### BR-001: No TeX installation is required

The manual is typeset inside a container image. No machine building it needs LaTeX, fonts or packages installed (NFR-002).

### BR-002: The stamp comes from a generated file

LaTeX reads the version and build date from a generated file rather than from the command line, so a hand-run build and a release build use identical sources. The generated file is not committed.

### BR-003: Unknown is written, not omitted

A build that supplies neither version nor date renders both as "unknown". The fields are never left blank (NFR-001).

### BR-004: The default brand, always

The manual is typeset in the Elicit default brand regardless of which brand any deployment mounts, using the same LaTeX brand package as the author's manual, so the two documents read as one set (NFR-003, C-003).

### BR-005: The manual is not packaged into any image

The PDF is a release artifact of the umbrella repository. No module image carries it and no application serves it (C-001).

### BR-006: Drift from the code is detectable

The configuration reference is checkable against the modules' sources by a script in the same directory, so a stale default is found by running something rather than by reading (NFR-004, NFR-005).
