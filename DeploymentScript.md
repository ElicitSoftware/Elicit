# How to Deploy the Elicit System with FHHS Data.

### Follow these steps for deploying in a non-docker system. 

### Create the database schemas
   1) Run the scripts in the postgresql/init_scripts folder. Don't forget to change the passwords! 

### Adjust the properties for your deployment. 
The docker-compose.yml has many properties defined for a local development environment.
Examine these properties and adjust them for your unique installation requirements. 
Refer to the Quarkus.io documentation for explanations of properties (https://quarkus.io/guides/all-config).

Three settings have no usable default from 3.0.0 and must be set by every deployment:

- **Database passwords.** Survey and FHHS ship without them. Set `ELICIT_DB_PASSWORD` and
  `ELICIT_OWNER_DB_PASSWORD` (or `quarkus.datasource.password` and
  `quarkus.datasource.owner.password`). A module started without them logs
  `no password was provided`, retries for about eight minutes and exits.
- **Survey's address for Admin.** Admin calls Survey to build reporting schemas and print the
  built-in respondent summary. Set `elicit.survey.url` to an address Admin can reach; the default,
  `http://survey:8080`, resolves only inside docker compose. Those `/api` paths on Survey are for
  Admin only, so do not publish them: a public Survey host should refuse every `/api` path except
  `/api/pdf/download`.
- **Trusted certificates.** Outbound HTTPS is verified (`quarkus.tls.trust-all` is no longer set).
  If a report service, post-survey action or identity provider uses a certificate from a private
  CA, add that CA with `quarkus.tls.trust-store.pem.certs`.

### Start the modules in this order to insure the database is properly setup and configured. 
We use Flyway for database setup and migration (https://www.red-gate.com/products/flyway/community/).
To insure the database populates please start the modules in this order. 
Wait for each module to report ready on `/q/health/ready` before starting the next.
1) Start Survey
   This will install most of the tables needed for a survey but no survey
2) Start Admin
   This will install all the tables needed for the Admin app. No department is created: the first administrator to sign in is asked to create one (it is assigned to them), and a user with no department can only log out.
3) Start FHHS
   This installs FHHS's own tables, grants and indexes. It no longer installs the Family History Survey: FHHS starts, reports not-ready on `/q/health/ready` and refuses report requests until the survey has been imported (step 5). A failed start can simply be started again once the cause is fixed.
   Start it after Admin, not beside it: on a fresh database the two modules' first migrations grant on the same tables, and run together one of them fails with "tuple concurrently updated".
4) Start Pedigree
   This is the module for generating a visual pedigree.
5) Import the Family History Survey
   Sign in to Admin as an administrator, create a department if asked, then open Apply Survey Definition and upload `FHHS/family-history-survey.elicit`. The apply rebuilds Survey's reporting schema, and FHHS becomes ready on its next health probe; nothing needs restarting. Do the same on every site that runs the Family History Survey.

### Choosing which languages a site offers

The Survey and Admin images carry their user-interface texts in **English only**. Every other
language is a translation file the site mounts. ElicitSoftware maintains a curated set beside each
release in this repository's `elicit-i18n/`: Latin American Spanish (`es-419`) and Arabic (`ar`)
for both applications, plus `i18n-config.json` for text direction and size. With nothing mounted, a
site offers English only and shows no language selector.

To offer the curated languages, mount `elicit-i18n/` read-only into both services and point them at
it:

```
volumes:
  - ./elicit-i18n/:/opt/i18n:ro
environment:
  i18n.file.system.path: /opt/i18n   # Survey reads /opt/i18n/survey/, Admin /opt/i18n/admin/
```

Without Docker, copy the directory to the server and set `i18n.file.system.path` to its location.
Use the set from the same release as the images: each application's tests check the files against
that version's English texts.

To offer fewer languages than are mounted, set one property per service:

```
i18n.offered.locales=en,es-419    # English and Latin American Spanish; a mounted Arabic is ignored
i18n.offered.locales=en,ar        # English and Arabic, laid out right to left
```

Set it through any Quarkus configuration source, such as an environment entry in the compose file,
a system property, or `application.properties`. A language left out is unreachable: it is absent
from the selector, refused in a `?lang=` link, and not served for survey content either, because a
survey's content is only served in a language the site also offers for the interface. English is
always offered. A tag listed with no mounted file is not offered, and the service logs a warning.
`i18n.bundled.locales`, which earlier 3.0.0 builds used, no longer exists.

A site may also supply its own translations:

- **Add a language:** put `translations_<tag>.properties` in both `survey/` and `admin/` (file names
  use underscores, as in `translations_pt_BR.properties`). Each module's `i18n/TRANSLATION_REQUEST.md`
  is the document to hand a translator or an AI agent to produce one.
- **Change some texts:** a file needs only the keys it changes; every missing key falls back to
  English.

Restart the services after changing the mount.

Adjusting typography for one language: declare it in the mount's `i18n-config.json`, which applies
to both applications, or override it for one service by property:

```
i18n.font-scale.ar=1.2            # Arabic ships at 1.15
i18n.direction.<tag>=rtl|ltr      # only for a language whose script the release does not know
```

The organization name shown in the header is translated in the brand directory instead
(`localized` block in `brand-config.json`, see `elicit-brand/README.md`).

### Upgrading an existing deployment to Kimball Type 2 SCD (V3.0.0)

Survey's V3.0.0 release (Survey UC-002 BR-009; the `scd` spec tests are its executable record) versions every structural
survey table (questions, sections, steps, relationships, etc.) with durable keys and
effective-dated rows. It creates several new Postgres sequences
(`survey.questions_durable_seq` and seven others) that two *other* apps' own future
migrations depend on:

- Admin's `V0.0.12__Add_Kimball_Durable_Seq_Grants.sql` grants `${surveyadmin_user}` access to
  those sequences.
- FHHS's `V0.0.8__REORDER_CANCER_QUESTIONS_DURABLE.sql`, a durable-key rewrite of
  `V0.0.5__UPDATE_CANCER_QUESTONS.sql`'s hardcoded surrogate ids, looks up a durable
  `section_id` this migration produces.

Both migrations ship in 3.0.0. FHHS's `V0.0.8` has been verified against a real
Survey-Kimball-migrated database and is idempotent; Admin's `V0.0.12` is a straightforward
grant. They must run *after* Survey's Kimball migration has completed on the shared database,
or they fail outright (the sequences and columns they reference do not exist yet) rather than
silently corrupt anything. FHHS's `V0.0.9` (durable-sequence sync) is applied on FHHS's second
start after an upgrade; the first start is already healthy, and nothing needs doing.

- **docker-compose deployments**: already safe as long as `survey`, `admin`, and `fhhs` are
  upgraded together in the same `docker compose pull && docker compose up -d` — `admin`
  declares `depends_on: survey: condition: service_healthy`, `fhhs` depends on both `survey`
  and `admin` being healthy, and Quarkus's `/q/health/ready` only reports healthy after
  Survey's own Flyway migration (`ManualSchemaMigrator`) has finished. **The one unsafe case**: upgrading Admin's or
  FHHS's image tag independently of Survey's (e.g. `docker compose up -d admin` alone)
  while Survey is still running an older, pre-Kimball image — `depends_on` only orders
  *startup*, it does not re-validate an already-running dependency. Always upgrade all
  three together for this release.
- **Non-docker (manual) deployments**: add "Survey's Kimball Type 2 SCD migration has
  completed" as an explicit precondition before starting/restarting Admin or FHHS on a
  V3.0.0-or-later Survey database — there is no automatic health-check gating here, so this
  is on the operator to sequence correctly, same as the existing Survey → FHHS ordering
  above.
- **Rollback**: none of the three apps ships a Flyway down-migration for their Kimball
  work (see each repo's own research doc) — recovery from a bad rollout is an operational
  pre-upgrade database backup/restore, not a code-level rollback.
- **Collation version after a host change**: if the database moves to a host or image with a
  different glibc during the upgrade (a new base image, a restore onto another server),
  PostgreSQL warns `database "survey" has a collation version mismatch` on every connection,
  and indexes on text columns may order differently until rebuilt. Before starting the
  applications, in each database the warning names (usually `survey`, `postgres` and
  `template1`), as a superuser:

  ```sql
  REINDEX DATABASE survey;
  ALTER DATABASE survey REFRESH COLLATION VERSION;
  ```

  `resetDatabase.sh V2` does the same to the `PGDATA_v2` test copy, which was made under
  glibc 2.41 while the `elicit_db` image provides 2.36.
- **Temporary upgrade-path scaffolding — remove once the V2→V3 rollout is complete.** Both
  Survey and FHHS implement the V2.x→V3 upgrade via a `com.elicitsoftware.flyway.
  ManualSchemaMigrator` class that routes each boot to either `db/migration` (the current,
  Kimball-native schema) or a frozen `db/migration-v3/` copy (the pre-Kimball history),
  based on Flyway checksum validation. **This entire mechanism — the `ManualSchemaMigrator`
  class in each repo, both `db/migration-v3/` directories, and their dedicated upgrade-path
  tests — exists only to support databases that haven't upgraded yet.** Once every real
  Survey and FHHS deployment has converged onto `db/migration` (each app logs this when it
  happens), delete all of it; see the `README.md` inside each `db/migration-v3/` directory for
  the exact file list. Track this as a real follow-up once the rollout is confirmed
  complete — don't let it get pulled into builds forever out of inertia.

### Upgrading to one reporting schema per survey (Survey V021)

Before V021 the reporting star lived in `surveyreport` and worked for exactly one survey, the
one whose `survey.surveys.id` was 1. Survey V021 gives every survey a star schema of its own
(Survey UC-008): `surveyreport` keeps only the shared `dim_date` and `dim_status`, and each
survey's `dim_step`, `dim_section`, `dim_question`, `dim_item`, `dim_<tag>` tables,
`fact_sections` and views live in a schema named `report_<slug of the survey name>`, recorded on
`survey.surveys.report_schema`.

On an existing deployment the upgrade is a **drop and regenerate**, not a migration:

1. Survey's `db/migration-v3/V021__Report_Schema_Per_Survey.sql` runs at Survey's first start on
   the new release. It drops the two `fact_respondents` triggers and their functions, drops
   every object in `surveyreport` except `dim_date`, `dim_status` and `dim_status_seq` (the
   fact tables, both views, `dim_step`, `dim_section`, every `dim_<tag>` and their sequences —
   FHHS's indexes on `fact_sections` go with the table), and adds `surveys.report_schema`.
2. The same startup then builds every installed survey: names its schema, creates it, and
   back-fills `fact_sections` from `survey.answers` for every finalized respondent.
   `fact_respondents` is a view from now on, so it needs no back-fill and no trigger.

What to expect and to tell the people who use the reporting schema:

- **Surrogate ids change.** `fact_sections.id` and every `dim_*.id` are reassigned by the
  regeneration. An extract or a BI model keyed on them must be pulled again; one keyed on
  `respondent_id` and the dimension values is unaffected.
- **The schema name changes.** Queries and BI connections that read
  `surveyreport.fact_sections_view` must read `<report_schema>.fact_sections_view` instead —
  for the Family Health History Survey, `report_family_history_survey`. Look the name up on
  `survey.surveys.report_schema`; it is site-local and can be renamed afterwards
  (`POST /api/etl/schema/<key>/rename?name=` on Survey), and nothing inside Elicit hard-codes
  it (FHHS resolves it at query time).
- **Two columns are now correct that were not.** `fact_sections.step_key` and `section_key`
  held display orders; they now hold the dimension ids as of the respondent's first access, so
  `section` in the view names the right section for every row, not only a step's first.
- **The drop and the regeneration are not one transaction.** A Survey start that fails after
  V021 ran (a build error, a crash) leaves the site with no reporting tables until a build
  succeeds. Nothing is lost — every row is rebuilt from `survey.answers`, and a restart or
  `POST /api/etl/build` completes it — but report consumers see empty results until then.
  Take the usual pre-upgrade backup and upgrade outside reporting hours.
- **Order.** As for the Kimball upgrade above, Survey must finish starting before FHHS and
  Admin on the shared database. FHHS's greenfield `V0.0.6` no longer creates indexes on
  `surveyreport.fact_sections`; the fact-table indexes are created per survey schema by Survey.
- **Preview instances.** An instance running with `elicit.etl.enabled=false` (Author's
  preview) builds nothing and holds no schema; leave it so.
- **Rollback**: none. Recovery is the pre-upgrade backup.

### Upgrading to respondent progress stamps (Survey V022)

Survey V022 (Survey UC-012) adds two nullable columns, `survey.answers.presented_dt` (when the
page holding the row was first rendered) and `survey.respondents.last_activity_dt` (the
respondent's latest page render or save), and changes what `answers.saved_dt` means: from this
release it is set only when the respondent changes a value, never for a question's default.
The migration runs on both tracks at Survey's first start and back-fills what the database can
prove: every answer row of a **finished** respondent gets `presented_dt` from its `created_dt`,
because finishing passed every page, and everyone who has logged in gets `last_activity_dt`
from their finalization, latest save or first access. Rows of respondents still in progress are
left unstamped, and existing `saved_dt` values are left as they are. Nothing else changes:
no reporting schema is touched, and the respondent export and import (Admin UC-011/UC-012)
carry the two columns as a trailing field, so a file exported before this release still imports.

### Upgrading to access codes

The credential a respondent enters to reach a survey used to be called a "token". It is now the
**access code** everywhere, with no backwards-compatible aliases. This is a breaking change that
spans Survey, Admin and FHHS, so upgrade all three from the same release.

- **Deploy order: Survey, then Admin, then FHHS.** Survey's `V014` renames
  `survey.respondents.token` to `access_code` (with its unique constraint and indexes). Admin's
  `V0.0.17` then renames the `survey.status` view's output column to `access_code`, and FHHS reads
  that view. An Admin or FHHS build from this release fails against a database Survey has not yet
  migrated, and an older Admin or FHHS build fails against one it has. With docker compose,
  upgrade all three together (`depends_on` orders their startup). Manual deployments must finish
  Survey's migration before starting Admin and FHHS.
- **Admin edits two applied migrations.** `V0.0.1` and `V0.0.7` now select `r.access_code`, so a
  fresh database can create the `survey.status` view. `quarkus.flyway.owner.repair-at-start=true`
  rewrites their recorded checksums on existing databases; no manual `flyway repair` is needed.
- **Configuration key:** `token.autoRegister` is now `accessCode.autoRegister`. Rename it in any
  environment that sets it; the old key is ignored.
- **Email templates:** the placeholder `<TOKEN>` is now `<ACCESS_CODE>`. Admin's `V0.0.18`
  converts every stored template automatically. Templates added later must use `<ACCESS_CODE>`;
  `<TOKEN>` is left in the message as literal text.
- **SFTP XML template:** the placeholder `{Token}` in `family.history.sftp.xml.template` is now
  `{AccessCode}`. This value comes from deployment configuration, so update it by hand; `{Token}` is
  no longer substituted.
- **Integration API:** the JSON returned by `/api/secured/add/subject`, `/add/subjects` and
  `/add/csv` names the credential `status.accessCode` instead of `status.token`. Update API clients
  before upgrading. The smoke-test endpoint `/api/secured/test` now returns
  `access code service test`.
- **Admin links:** the subject edit route takes `?accessCode=` instead of `?token=`.
- **Respondent sessions:** the session attribute was renamed, so a respondent who is mid-survey
  during the upgrade must log in again with their access code. Their answers are kept.
- **Respondent export files** keep the `ELICIT_EXPORT_V1` format and still import. New exports
  label the header line `# access_code:` instead of `# token:`; the importer ignores that line.
- **Branding:** the CSS class `.elicit-token` is now `.elicit-access-code`, and the variables
  `--brand-token-bg`/`--brand-token-text` are now `--brand-access-code-bg`/`--brand-access-code-text`.
  Rename them in any custom brand that overrides them.

### Check the deployment from the Admin console
Once Admin is up, sign in as an administrator and open **System** in the navigation. Its
pages show what the service is running, whether both database connections and each module's
migrations are in place, which brand directory resolved and where each brand asset came from,
the effective mail settings with a "Send test email" button, and a bounded reachability check
of every outbound dependency (identity provider, report services, post-survey actions, mail
relay, telemetry collector), plus an OIDC page with the current sign-in's roles and masked tokens.
Secrets are shown as present or absent only. The **Overview** page
lists the setup work still outstanding, including the seeded accounts below.

### Modify template data
After starting a new Elicit system you will need to alter some of the template data. 
1) Update test users to real users. 
   The migrations seed two accounts, `admin` and `user`. Admin logs a warning at every start
   and shows a banner on every console screen until both are renamed; under
   `elicit.authorization.mode=DATABASE` rename the matching identity-provider accounts to the
   same usernames. Rename them under Admin | Users or with this SQL:
   UPDATE survey.users set username = '< username >', first_name = '< first_name >', last_name = '< last_name >' where id = 1; 
   UPDATE survey.users set username = '< username >', first_name = '< first_name >', last_name = '< last_name >' where id = 2; 

2) Create a new Department and assign users. 
   This can be done in the UI under Admin | Departments and Admin | Users or you can use this SQL
   INSERT INTO survey.departments(id, name, code, default_message_id, from_email) 
   VALUES(NEXTVAL('survey.departments_seq'),'< name >','< code >', '1','< from_email >');

3) Update the email message template
   This can be done in the ui under Admin | Message Templates
   Use the placeholder <ACCESS_CODE> wherever the subject's access code belongs, e.g. in the
   login link: https://<your survey host>/#/login/<ACCESS_CODE>

4) Post Survey Actions. 
   If you would like to automatically upload the family history report to your system you can use a post survey action. 
   The default system is configured to send via sftp two documents a meta data file named <Xid>-index.xml and the report <Xid>.pdf
   the contents of the xml file can be defined with this property family.history.sftp.xml.template it can include these values which will be populated at the time it is created: 
        {Xid}
        {RespondentId}
        {SurveyId}
        {FirstName}
        {LastName}
        {MiddleName}
        {Dob}
        {Email}
        {Phone}
        {DepartmentName}
        {DepartmentID}
        {AccessCode}
        {Status}
        {Created}
        {Finalized}
    Here is an example xml template:
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <pdf>
                <externalSourceId>{RespondentId}</externalSourceId>
                <mrn>{Xid}</mrn>
            </pdf>
            
    If you wish to disable this remove the line in the post_survey_actions table. 
    DELETE FROM survey.post_survey_actions WHERE id = 1;
    Or if you would like to use it then you will need to adjust the host of the URL of the action. 
    UPDATE survey.post_survey_actions set url = 'http://host.docker.internal:8082/familyhistory/generate' where id = 1;
    
    You will also have to update the sftp properties. From 3.0.0 delivery is off unless
    enabled, and the server's host key is verified: FHHS refuses to connect until
    family.history.sftp.knownHosts holds the server's known_hosts lines (the output of
    `ssh-keyscan -p <port> <host>`) or the path of a known_hosts file. Setting
    family.history.sftp.strictHostKeyChecking=false skips the check; use it only for a local
    development server.
      - family.history.sftp.enabled=true
      - family.history.sftp.knownHosts=<known_hosts lines or file path>
      - family.history.sftp.host=sftpServer
      - family.history.sftp.username=fhhs_user
      - family.history.sftp.password=pass
      - family.history.sftp.path=/upload/reports
      - family.history.sftp.port=22
      - family.history.upload.psa.id=1 

5) Update Report URLs
   The reporting system also uses URLs to communicate. 
   Update the host of the URLs in the reports table. 
   
   UPDATE survey.reports set url = 'http://host.docker.internal:8082/proband/report' WHERE id = 1;

   UPDATE survey.reports set url = 'http://host.docker.internal:8082/casummary/report' WHERE id = 2;

   UPDATE survey.reports set url = 'http://host.docker.internal:8082/pedigree/report' WHERE id = 3;

