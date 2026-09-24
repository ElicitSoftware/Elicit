# Multi-site end-to-end test

Tests the theory of one survey running at several Elicit sites that pick up survey revisions at
different times, with every site's respondents pulled back into the master site for the study
reports. Two complete stacks run side by side on this machine:

| | Site 1 (master) | Site 2 |
|---|---|---|
| Survey | http://localhost:8080 | http://localhost:8030 |
| Admin | http://localhost:8081 | http://localhost:8031 |
| FHHS | http://localhost:8082 | http://localhost:8032 |
| Pedigree | http://localhost:8083 | http://localhost:8033 |
| Author | http://localhost:8084 (preview 8085) | — (site 2 only imports) |
| Keycloak | http://localhost:8180 (admin/admin) | uses site 1's |
| Mailpit | http://localhost:8025 | uses site 1's |
| Jaeger | http://localhost:16686 | http://localhost:16636 |
| PostgreSQL | localhost:5452 | localhost:5402 |

Site 2's host ports are site 1's minus 50. Neither site publishes SMTP, SFTP or the Jaeger
collector ports: the apps reach those services by compose service name. Site 2 shares site 1's
Keycloak (the realm lists `http://localhost:8031/*` as an Admin redirect) and Mailpit (site 2's
Admin also joins site 1's compose network, so `mailpit` resolves there; its own database and
Jaeger are addressed by the aliases `site2-db` / `site2-jaeger` because on two networks the plain
names would be ambiguous). Both Admins sign in as `admin/admin`; each site keeps its own
`survey.users` row and department assignments.

## The story the test runs

`HouseholdSurveyMultisiteE2ETest` is one ordered story, one JUnit method per phase; a failed
phase marks the rest skipped so the report shows where the theory broke. Every persona visit
(author, each site's administrator, every respondent login) runs in a fresh browser context.

1. Site 1's author creates the **Household Survey** in Author and exports `household-v1.elicit`.
   Step 1 "The house" asks how many people live in the house and repeats "Name of person {Q#}"
   once per person (REPEAT rule); each name SHOWs Step 2 "Household member" once, carrying the
   name as token `NAME` into the section title "About {NAME|this person}" and the questions
   (age, sex, relationship, children). This is the FHHS sibling pattern: the runtime cannot repeat
   a whole step, it repeats the name question and shows the step once per name.
2. Site 1 applies v1 ("New Survey Installed").
3. Site 2 creates department "Site 2 Clinic" (code `SITE2`), assigns it to `admin`, applies v1.
4. Site 1 registers R1a, R1b, R1c. R1a finishes (two household members); R1b completes Step 1,
   sees "About Alice", leaves; R1c never logs in.
5. Site 2 does the same with R2a, R2b, R2c under Site 2 Clinic.
6. Site 1's author edits the sex question to gender (options Woman/Man), exports v2; site 1
   applies it ("Survey Updated").
7. Site 2, still on v1, registers R2d who finishes the survey and still reads "sex".
8. On site 1, R1b resumes and finishes, still on v1 (snapshot anchored to first access);
   R1c starts and reads "gender".
9. Site 2 applies v2.
10. On site 2, R2b resumes on v1 and finishes; R2c starts on v2.
11. Site 2 exports R2a–R2d one by one (Search › Export, `ELICIT_EXPORT_V2`).
12. Site 1 creates the same-coded department and imports the four files.
13. Site 1's search shows all seven respondents; the imported four are Finished, in Site 2
    Clinic, and re-exporting one from site 1 reproduces site 2's answer and dependent lines.

Access codes are read from the Admin search grid, as in `../e2e-tests`.

## Prerequisites

- Docker Desktop; the module images built from the umbrella root (`./buildDockerImages.sh`).
  Admin needs the portable respondent export/import (`ELICIT_EXPORT_V2`) and Author the rule
  dialog that stores a token on any action and same-section question rules question-only.
- Playwright's Chromium, installed once:
  `mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"`
  (from `../e2e-tests` or here after a first `mvn test-compile`).
- The umbrella stack stopped (`docker compose down` in the repo root): site 1 uses the same ports.

## Running

```bash
./run.sh             # reset both databases, start both sites, run the journey
```

or step by step:

```bash
./reset.sh all       # wipe data/ (PGDATA, Mailpit, SFTP, Keycloak copy) of both sites
./up.sh              # site 1, then site 2; a first start initialises both databases (2–3 min each)
./status.sh          # HTTP probe of every URL above
mvn -DskipTests=false test               # the journey (add -De2e.headless=false to watch)
./down.sh            # stop both
```

The journey installs the one **Household Survey**: one survey key across both revisions and
both sites, so the star schema keeps the same dimensions through the update. That is why the
databases are cleared between runs; phase 1 refuses to start if the survey already exists in
Author. Exported `.elicit` files land in `target/exports/<timestamp>/`; a failing phase writes
`target/failure-<time>.png`.

Base URLs can be overridden with `-Dsite1.survey.baseUrl=…`, `-Dsite1.admin.baseUrl`,
`-Dsite1.author.baseUrl`, `-Dsite2.survey.baseUrl`, `-Dsite2.admin.baseUrl`; credentials with
`-Dadmin.username/-Dadmin.password/-Dauthor.username/-Dauthor.password`.

## Layout

- `site1/docker-compose.yml`, `site2/docker-compose.yml` — self-contained copies of the umbrella
  compose file (see the comments at the top of each). Data lives under `data/<site>/` (ignored).
- `site2/post-init.sql` — FHHS seeds report and post-survey-action URLs with host port 8082
  (site 1's FHHS); `up.sh` rewrites them to 8032 after site 2's first start.
- `pom.xml` — borrows the page objects of `../e2e-tests` by source (build-helper) and runs only
  `**/multisite/*E2ETest.java`.
- `src/test/java/com/elicitsoftware/e2e/multisite/` — `Site`, `MultisiteTestBase`,
  `HouseholdSurvey` (the survey and deterministic answers) and the journey.

## Known limits

- Author runs on site 1 only; site 2 imports definitions and exports respondents.
- The reporting star (`surveyreport`) is only populated for survey id 1 (the FHHS survey); the
  imported respondents are complete in `survey.*` but do not appear in FHHS reporting.
- The Survey runtime hides, at login, every question of the first step's sections that a rule
  names as its downstream section or step; Author therefore stores same-section question rules
  question-only. A rule in the first step that targets a question in another section of that
  step is not covered.
- `surveyreport.dim_step.value` and `dim_section.value` are unique per site: two different
  surveys sharing a step or section dimension name make the Survey ETL fail (and the Survey app
  fail to start). Revisions of one survey are fine, since dimension rows are keyed by the
  durable step/section id.
- Both sites must share the same `.elicit` lineage: element keys and versions are what the
  respondent import resolves; a survey authored separately on each site cannot be merged.
- The respondent import still parses `answers.step` / `answers.section` as integers; a survey
  with decimal display orders (midpoint insertion) is not covered by this test.
- Greenfield databases only; the V2 brownfield path of `../resetDatabase.sh` is not exercised.
