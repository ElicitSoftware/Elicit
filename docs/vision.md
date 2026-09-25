# Elicit Platform — Vision (Umbrella)

This is the vision document for the **umbrella repository**: Elicit considered as
one deployable platform rather than as five independent modules. Each module
(`Survey/`, `Admin/`, `Author/`, `FHHS/`, `Pedigree/`) carries its own
`docs/vision.md` for its own behavior. This document covers only what is true of
the platform *as a whole*, and only the concerns no single module owns.

## The problem

Elicit ships as separate module images that share one PostgreSQL database, one
identity provider, one brand directory and one translations directory. Nothing
in any module's repository describes that whole. An operator standing up a site
today has to reconstruct it from `docker-compose.yml`, from
`DeploymentScript.md`, from two implementation guides written for developers,
and from three `application.properties` files totaling some 760 lines in which
the Elicit-specific settings are interleaved with Quarkus defaults.

The consequences are concrete and have all been observed:

- The start order matters (Survey creates the shared `survey` schema; Admin and
  FHHS migrate on top of it) and is recorded only in a compose comment and a
  prose paragraph.
- Database roles and schemas are created by the `elicitsoftware/elicit_db`
  image's init scripts. `DeploymentScript.md` points a non-Docker installer at a
  `postgresql/init_scripts` directory that no longer exists.
- Which settings an operator is *expected* to change, and which are internal
  defaults that happen to be in the same file, is nowhere stated.
- Branding and translations are both externally mounted directories with a
  defined layout and a defined fallback order, documented in guides whose
  audience is the developer implementing them, not the operator mounting them.

## The product

**The Elicit Installation Manual** — a printable, versioned PDF that takes an
operator from an empty machine to a running, verified Elicit site, and is the
single reference for every setting that site is configured with.

It is typeset from LaTeX in the Elicit default brand, matching the author's
manual (`Author/docs/manual/`), and built by a script in this repository so that
it can be stamped with a platform version and a build date and reproduced
exactly.

## Scope

**In scope** — provisioning a deployment:

- The platform topology: which modules exist, what each one needs, what they
  share, and in what order they may be started.
- The PostgreSQL cluster: roles, schemas, the `survey` and `author` databases,
  and the grants the migrations assume.
- The identity provider: the OIDC clients and the roles the applications check.
- Both installation paths: Docker Compose, and module-by-module without Docker.
- The configuration reference for Survey, Admin and Author: every
  Elicit-specific setting with its default, and the Quarkus settings the images
  actually set, with a pointer to `quarkus.io` for the rest.
- External branding: the directory layout, the fallback rules, and how to mount
  one.
- Translations: the directory layout, the resolution order, and how a deployment
  adds, replaces or removes a language.
- Verifying the result, and where to go when it is wrong.

**Out of scope** — operating a deployment once it runs:

- Running the console day to day (Admin's administrator's manual, Admin UC-029).
- Building a survey (Author's manual, `Author/docs/manual/`, Author UC-039).
- FHHS and Pedigree configuration beyond what Survey, Admin and Author need in
  order to reach them. FHHS is specific to one survey and carries its own
  deployment documentation.
- Kubernetes, cloud load balancers, TLS termination and backup policy: site
  infrastructure, not Elicit configuration.
- Upgrade procedures, which stay in `DeploymentScript.md`; the manual points at
  them rather than duplicating them, because they are release-specific and this
  manual is not.

## Audience

The **deployment operator**: someone comfortable with PostgreSQL, a container
runtime or a JVM service manager, and an OIDC provider, who has not seen Elicit
before. Not a developer of it, and not a survey administrator.

## What success looks like

An operator who has the PDF, the module images and a database server can bring
up a working site without reading any module's source, and can answer "what is
this setting and what happens if I change it?" for every setting their site sets.
