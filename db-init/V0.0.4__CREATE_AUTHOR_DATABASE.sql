-- Author's working database: a second database in the same Postgres container so the
-- authoring tool never touches the site's survey data. Same roles (they are cluster-wide,
-- created by the image's V0.0.1), same schemas and grants as the image gives `survey`
-- (V0.0.2). The survey schema itself is created by the author-survey container's Flyway
-- migrations, exactly as the survey container does for the site database.
--
-- Mounted into /docker-entrypoint-initdb.d/, so it runs only when the cluster is first
-- initialised (empty ./postgresql/PGDATA). To add the database to an existing cluster:
--   docker compose exec db psql -U survey -d survey -f /docker-entrypoint-initdb.d/V0.0.4__CREATE_AUTHOR_DATABASE.sql
CREATE DATABASE author;
GRANT CONNECT ON DATABASE author TO dbowner;
GRANT CREATE ON DATABASE author TO dbowner;
GRANT CONNECT ON DATABASE author TO dbuser;
\c author

GRANT USAGE, CREATE ON SCHEMA public TO dbowner;
GRANT USAGE ON SCHEMA public TO dbuser;

CREATE SCHEMA survey AUTHORIZATION elicit_owner;
GRANT USAGE ON SCHEMA survey TO survey_user;
GRANT USAGE ON SCHEMA survey TO surveyadmin_user;
GRANT USAGE ON SCHEMA survey TO surveyreport_user;

CREATE SCHEMA surveyreport AUTHORIZATION elicit_owner;
GRANT USAGE ON SCHEMA surveyreport TO surveyreport_user;
GRANT USAGE ON SCHEMA surveyreport TO surveyadmin_user;
GRANT USAGE ON SCHEMA surveyreport TO survey_user;

GRANT USAGE, CREATE ON SCHEMA surveyreport TO dbowner;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA surveyreport TO dbowner;
ALTER DEFAULT PRIVILEGES IN SCHEMA surveyreport GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO dbowner;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA surveyreport TO dbowner;
ALTER DEFAULT PRIVILEGES IN SCHEMA surveyreport GRANT USAGE ON SEQUENCES TO dbowner;
