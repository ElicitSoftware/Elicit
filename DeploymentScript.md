# How to Deploy the Elicit System with FHHS Data.

### Follow these steps for deploying in a non-docker system. 

### Create the database schemas
   1) Run the scripts in the postgresql/init_scripts folder. Don't forget to change the passwords! 

### Adjust the properties for your deployment. 
The docker-compose.yml has many properties defined for a local development environment.
Examine these properties and adjust them for your unique installation requirements. 
Refer to the Quarkus.io documentation for explanations of properties (https://quarkus.io/guides/all-config).

### Start the modules in this order to insure the database is properly setup and configured. 
We use Flyway for database setup and migration (https://www.red-gate.com/products/flyway/community/).
To insure the database populates please start the modules in this order. 
1) Start Survey
   This will install most of the tables needed for a survey but no survey
2) Start FHHS
   This will install the survey but fail Migrating schema "survey" to version "0.0.3 - CREATE FHHS FACT VIEW" 
3) Restart Survey
   Now that there is a survey in place the reporting schema will be populated including the surveyreport.fact_sections_view which the FHHS FACT VIEW is built on. 
4) Restart FHHS
   This will complete the database migrations.
5) Start Admin 
   This will install all the tables needed for the Admin app. It will also create a "Test Department". This test department can be used to test the new installation or upgrades. It is recommended that you keep this test department for future testing. 
6) Strat Pedigree
   This is the module for generating a visual pedigree.

### Modify template data
After starting a new Elicit system you will need to alter some of the template data. 
1) Update test users to real users. 
   UPDATE survey.users set username = '< username >', first_name = '< first_name >', last_name = '< last_name >' where id = 1; 
   UPDATE survey.users set username = '< username >', first_name = '< first_name >', last_name = '< last_name >' where id = 2; 

2) Create a new Department and assign users. 
   This can be done in the UI under Admin | Departments and Admin | Users or you can use this SQL
   INSERT INTO survey.departments(id, name, code, default_message_id, from_email) 
   VALUES(NEXTVAL('survey.departments_seq'),'< name >','< code >', '1','< from_email >');

3) Update the email message template
   This can be done in the ui under Admin | Message Templates

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
        {Token}
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
    
    You will also have to update the sftp properties. 
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

