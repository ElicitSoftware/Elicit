-- Removes everything synth-fhhs.sql created (fact rows first: no delete trigger on respondents).
\set ON_ERROR_STOP on
BEGIN;
DELETE FROM surveyreport.fact_sections    WHERE respondent_id IN (SELECT id FROM survey.respondents WHERE access_code LIKE 'SYN%');
DELETE FROM surveyreport.fact_respondents WHERE id            IN (SELECT id FROM survey.respondents WHERE access_code LIKE 'SYN%');
DELETE FROM survey.answers  WHERE respondent_id IN (SELECT id FROM survey.respondents WHERE access_code LIKE 'SYN%');
DELETE FROM survey.subjects WHERE respondent_id IN (SELECT id FROM survey.respondents WHERE access_code LIKE 'SYN%');
DELETE FROM survey.respondents WHERE access_code LIKE 'SYN%';
COMMIT;
SELECT (SELECT count(*) FROM survey.respondents) respondents, (SELECT count(*) FROM surveyreport.fact_sections) fact_sections;
-- Undo the dimension-name de-duplication applied so POST /api/etl/build could run on this multi-survey dev db.
UPDATE survey.steps    SET dimension_name = 'Welcome' WHERE survey_id <> 1 AND dimension_name LIKE 'Welcome (s%';
UPDATE survey.sections SET dimension_name = 'Basics'  WHERE survey_id <> 1 AND dimension_name LIKE 'Basics (s%';
DELETE FROM surveyreport.dim_step    WHERE value LIKE 'Welcome (s%';
DELETE FROM surveyreport.dim_section WHERE value LIKE 'Basics (s%';
