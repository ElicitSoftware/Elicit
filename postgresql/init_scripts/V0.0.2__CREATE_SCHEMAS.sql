CREATE SCHEMA survey AUTHORIZATION elicit_owner;
GRANT USAGE on SCHEMA survey to survey_user;
GRANT USAGE on SCHEMA survey to surveyadmin_user;
GRANT USAGE on SCHEMA survey to surveyreport_user;

CREATE SCHEMA surveyreport AUTHORIZATION elicit_owner;
GRANT USAGE on SCHEMA surveyreport to surveyreport_user;
GRANT USAGE on SCHEMA surveyreport to surveyadmin_user;
GRANT USAGE on SCHEMA surveyreport to survey_user;

GRANT USAGE, CREATE ON SCHEMA surveyreport TO dbowner;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA surveyreport TO dbowner;
ALTER DEFAULT PRIVILEGES IN SCHEMA surveyreport GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO dbowner;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA surveyreport TO dbowner;
ALTER DEFAULT PRIVILEGES IN SCHEMA surveyreport GRANT USAGE ON SEQUENCES TO dbowner;
