# Use Case: Prepare the Database Cluster

## Overview

**Use Case ID:** UC-005  
**Use Case Name:** Prepare the Database Cluster  
**Primary Actor:** Deployment Operator  
**Secondary Actor:** PostgreSQL Cluster  
**Goal:** Create the login roles, databases and schemas Elicit's migrations assume, with site passwords, so that the modules can create their own tables on first start.  
**Status:** Open  
**Requirements:** [FR-008, FR-021, C-007](../requirements.md)

## Preconditions

- A PostgreSQL 17 cluster exists and the operator can connect to it as a superuser.
- The operator has decided which modules the site will run (UC-003).

## Main Success Scenario

1. The operator reads the database chapter, which states what Elicit creates for itself and what it expects to already exist.
2. The system gives the two group roles and the four login roles, with what each login role is used by: the schema owner used for migrations, the role the survey application connects as, the role the console connects as, and the role the reporting schema is read with.
3. The operator runs the given role script against the cluster, substituting a site password for each login role.
4. The operator creates the site's survey database and runs the given schema script in it, which creates the two schemas owned by the schema owner and grants each role usage.
5. The system states that no table is created here: every table, sequence, index and grant inside those schemas comes from a module's migrations on first start.
6. The operator records each role's password for the configuration step (UC-011).
7. The operator proceeds to the identity provider (UC-006).

## Alternative Flows

### A1: The site uses the bundled database image

**Trigger:** The operator installs with the container path and keeps the bundled database (step 3)  
**Flow:**

1. The image runs the same role and schema scripts on first initialization of an empty data directory, so steps 3 and 4 are already done.
2. The manual states that those scripts carry documented development passwords, and that a site keeping the image must change them (UC-013).
3. Use case continues at step 6.

### A2: The cluster's collation version does not match

**Trigger:** The cluster is restored from a copy made under a different operating system (step 4)  
**Flow:**

1. Creating a database fails on a collation version mismatch.
2. The manual gives the refresh statement to run against the template database, then repeats the creation.
3. Use case continues at step 4.

### A3: The schema owner is not a superuser

**Trigger:** Site policy forbids a superuser login role (step 3)  
**Flow:**

1. The manual states which privileges the migrations actually require of the schema owner — creating and altering objects in the two schemas, and creating extensions if the site's PostgreSQL requires them — so that the role can be narrowed deliberately.
2. The manual states that a narrowed role is the operator's to verify, because the modules' migrations are tested against the role the scripts create.
3. Use case continues at step 4.

## Postconditions

### Success Postconditions

- The cluster carries the login roles with site passwords and the site's survey database, with an empty `survey` and `surveyreport` schema owned by the schema owner. The schema owner holds `CREATE` on the database: each survey's own reporting schema (`report_<slug>`, Survey UC-008) is not created here but by Survey, the first time the survey is built after it is applied.

### Failure Postconditions

- The cluster is left in whatever state the failing statement reached. The scripts are re-runnable after the objects they already created are dropped; the manual says which.

## Business Rules

### BR-001: Roles are cluster-wide, schemas are per database

The login roles are created once for the cluster and serve every Elicit database in it. The schemas and their grants are created per database.

### BR-002: Elicit creates no roles

No module creates or alters a database role. Every role exists before any module starts.

### BR-003: Migrations create the tables, the operator creates the schemas

The operator creates the two empty schemas and the grants on them. Everything inside them is created by the modules' migrations.

### BR-004: The schema owner is the migrating role

Migrations run as the schema owner, not as an application's own role. The application roles are granted access by the migrations themselves.

### BR-005: No documented password reaches a site

Every password in the manual's scripts is a placeholder. A site that leaves one as printed is running with a published credential (FR-021).
