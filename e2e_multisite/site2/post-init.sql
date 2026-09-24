-- Site 2 post-initialisation (run by ../up.sh once fhhs is healthy; idempotent).
-- FHHS's migrations seed the report and post-survey-action URLs with host port 8082, which is
-- site 1's FHHS on this machine. Point them at site 2's own FHHS (host port 8032) - the same
-- fix DeploymentScript.md prescribes for any deployment that is not on the stock ports.
UPDATE survey.reports
   SET url = replace(url, 'host.docker.internal:8082', 'host.docker.internal:8032')
 WHERE url LIKE '%host.docker.internal:8082%';
UPDATE survey.post_survey_actions
   SET url = replace(url, 'host.docker.internal:8082', 'host.docker.internal:8032')
 WHERE url LIKE '%host.docker.internal:8082%';
SELECT 'reports' AS "table", id, url FROM survey.reports
UNION ALL
SELECT 'post_survey_actions', id, url FROM survey.post_survey_actions
ORDER BY 1, 2;
