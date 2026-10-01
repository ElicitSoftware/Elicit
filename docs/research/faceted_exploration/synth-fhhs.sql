-- Synthetic finalized FHHS respondents for the faceted-exploration research (docs/research/faceted_exploration.md, 2026-09-28).
-- Writes survey.respondents / survey.subjects / survey.answers only; the ETL
-- (POST /api/etl/build) builds surveyreport.fact_sections from them and the
-- fact_respondent_insert trigger fills fact_respondents. Undo: synth-cleanup.sql.
\set ON_ERROR_STOP on
SET syn.n = :N;
SELECT setseed(0.42);

-- Current-version question lookup: (section durable id, sections_questions.display_order)
DROP TABLE IF EXISTS pg_temp.syn_q;
CREATE TEMP TABLE syn_q AS
SELECT sq.section_id, sq.display_order::int AS dord, sq.id AS sq_id, q.id AS q_id,
       qt.name AS qtype, q.text
FROM survey.sections_questions sq
JOIN survey.questions q ON q.question_id = sq.question_id AND q.effective_to > now()
JOIN survey.question_types qt ON qt.id = q.type_id
WHERE sq.survey_id = 1 AND sq.effective_to > now();

-- Sanity: the layout this script assumes (fails loudly if the definition differs)
DO $$
DECLARE r record;
BEGIN
  FOR r IN SELECT * FROM (VALUES
      (5,1,'INTEGER'), (5,7,'RADIO'), (5,8,'RADIO'), (5,14,'MULTI_SELECT'), (5,16,'RADIO'),
      (6,2,'RADIO'), (6,3,'INTEGER'), (6,4,'INTEGER'),
      (7,2,'RADIO'), (7,3,'INTEGER'), (7,4,'INTEGER'),
      (8,1,'RADIO'), (8,2,'INTEGER'), (8,3,'INTEGER'), (8,4,'RADIO'),
      (9,1,'RADIO'), (9,2,'INTEGER'), (9,3,'INTEGER'), (9,4,'RADIO'), (9,5,'RADIO'), (9,6,'RADIO'),
      (10,1,'RADIO'), (10,2,'INTEGER'), (10,3,'INTEGER'),
      (11,1,'RADIO'), (12,1,'RADIO'), (12,4,'RADIO'), (13,1,'RADIO'), (14,1,'RADIO'), (15,1,'RADIO'), (15,4,'RADIO'),
      (16,2,'CHECKBOX'), (16,3,'RADIO'), (16,4,'INTEGER'), (16,8,'RADIO'), (16,55,'TEXT'), (16,58,'CHECKBOX'), (16,59,'INTEGER')
    ) v(sec, dord, qtype)
  LOOP
    IF NOT EXISTS (SELECT 1 FROM syn_q q WHERE q.section_id = r.sec AND q.dord = r.dord AND q.qtype = r.qtype) THEN
      RAISE EXCEPTION 'FHHS layout mismatch: section % display_order % expected %', r.sec, r.dord, r.qtype;
    END IF;
  END LOOP;
END $$;

CREATE OR REPLACE FUNCTION pg_temp.syn_answer(
    p_rid int, p_step int, p_inst int, p_sect int, p_sec_id int, p_dord int, p_val text, p_ts timestamptz)
RETURNS void LANGUAGE plpgsql AS $$
DECLARE q record;
BEGIN
  IF p_val IS NULL THEN RETURN; END IF;
  SELECT * INTO q FROM syn_q WHERE section_id = p_sec_id AND dord = p_dord;
  IF NOT FOUND THEN RAISE EXCEPTION 'no question at section % order %', p_sec_id, p_dord; END IF;
  INSERT INTO survey.answers (id, survey_id, respondent_id, step, step_instance, section, section_instance,
      question_instance, section_question_id, question_id, question_version, display_key, display_text,
      text_value, deleted, created_dt, saved_dt)
  VALUES (nextval('survey.answers_seq'), 1, p_rid, p_step, p_inst, p_sect, 0, 0, q.sq_id, q.q_id, 0,
      format('%s-%s-%s-%s-%s-%s-%s', lpad('1',4,'0'), lpad(p_step::text,4,'0'), lpad(p_inst::text,4,'0'),
             lpad(p_sect::text,4,'0'), '0000', lpad(p_dord::text,4,'0'), '0000'),
      q.text, p_val, false, p_ts, p_ts);
END $$;

-- One relative: demographics section (+ Cancers section when they have cancer)
CREATE OR REPLACE FUNCTION pg_temp.syn_relative(
    p_rid int, p_step int, p_inst int, p_sec_id int, p_kind text, p_gender text, p_gen int, p_ts timestamptz)
RETURNS void LANGUAGE plpgsql AS $$
DECLARE
  o_living int; o_age int; o_death int; o_gender int; o_sib int; o_shared int;
  vital text; age int; gender text := p_gender; has_ca boolean; p_dead numeric; sibtype text;
  base numeric; drew boolean := false; site record; pick boolean; dx int; r numeric;
BEGIN
  -- question display orders per section kind
  IF p_kind = 'parent'      THEN o_living:=2; o_age:=3; o_death:=4; o_gender:=NULL; o_sib:=NULL; o_shared:=NULL;
  ELSIF p_kind = 'child'    THEN o_living:=1; o_age:=2; o_death:=3; o_gender:=4;    o_sib:=NULL; o_shared:=NULL;
  ELSIF p_kind = 'sibling'  THEN o_living:=1; o_age:=2; o_death:=3; o_gender:=4;    o_sib:=5;    o_shared:=6;
  ELSIF p_kind = 'grand'    THEN o_living:=1; o_age:=2; o_death:=3; o_gender:=NULL; o_sib:=NULL; o_shared:=NULL;
  ELSIF p_kind = 'auntuncle'THEN o_living:=1; o_age:=2; o_death:=3; o_gender:=4;    o_sib:=NULL; o_shared:=NULL;
  END IF;

  IF gender IS NULL THEN gender := CASE WHEN random() < 0.5 THEN 'Female' ELSE 'Male' END; END IF;

  p_dead := CASE p_gen WHEN -2 THEN 0.80 WHEN -1 THEN 0.35 WHEN 0 THEN 0.08 ELSE 0.02 END;
  r := random();
  vital := CASE WHEN r < p_dead THEN 'Deceased' WHEN r < p_dead + 0.04 THEN 'Unknown' ELSE 'Alive' END;
  age := CASE p_gen WHEN -2 THEN 60 + floor(random()*40) WHEN -1 THEN 40 + floor(random()*45)
                    WHEN 0 THEN 18 + floor(random()*60) ELSE 1 + floor(random()*45) END;

  IF random() > 0.05 THEN PERFORM pg_temp.syn_answer(p_rid, p_step, p_inst, 1, p_sec_id, o_living, vital, p_ts); END IF;
  IF vital = 'Deceased' THEN
     IF random() > 0.10 THEN PERFORM pg_temp.syn_answer(p_rid, p_step, p_inst, 1, p_sec_id, o_death, age::text, p_ts); END IF;
  ELSIF vital = 'Alive' THEN
     IF random() > 0.05 THEN PERFORM pg_temp.syn_answer(p_rid, p_step, p_inst, 1, p_sec_id, o_age, age::text, p_ts); END IF;
  END IF;
  IF o_gender IS NOT NULL AND random() > 0.03 THEN
     PERFORM pg_temp.syn_answer(p_rid, p_step, p_inst, 1, p_sec_id, o_gender,
             CASE WHEN random() < 0.01 THEN 'Other' ELSE gender END, p_ts);
  END IF;
  IF o_sib IS NOT NULL THEN
     sibtype := CASE WHEN random() < 0.90 THEN 'Full' ELSE 'Half' END;
     PERFORM pg_temp.syn_answer(p_rid, p_step, p_inst, 1, p_sec_id, o_sib, sibtype, p_ts);
     IF sibtype = 'Half' THEN
        PERFORM pg_temp.syn_answer(p_rid, p_step, p_inst, 1, p_sec_id, o_shared,
                CASE WHEN random() < 0.5 THEN 'Mother' ELSE 'Father' END, p_ts);
     END IF;
  END IF;

  -- cancers
  base := CASE p_gen WHEN -2 THEN 0.35 WHEN -1 THEN 0.22 WHEN 0 THEN 0.12 ELSE 0.03 END;
  has_ca := random() < base;
  IF NOT has_ca THEN RETURN; END IF;
  PERFORM pg_temp.syn_cancers(p_rid, p_step, p_inst, gender, age, p_ts);
END $$;

-- The shared Cancers section (durable 16, section display order 2)
CREATE OR REPLACE FUNCTION pg_temp.syn_cancers(p_rid int, p_step int, p_inst int, p_gender text, p_age int, p_ts timestamptz)
RETURNS void LANGUAGE plpgsql AS $$
DECLARE
  site record; drew boolean := false; pass int := 0; rate numeric; dx int;
BEGIN
  WHILE NOT drew AND pass < 5 LOOP
    pass := pass + 1;
    FOR site IN SELECT * FROM (VALUES
        ('bladder',    2,  3,  4, 0.03, 'B'), ('breast',     5,  6,  7, 0.30, 'F'), ('colon',     9, 10, 11, 0.15, 'B'),
        ('endometrial',12, 13, 14, 0.06, 'F'), ('kidney',    15, 16, 17, 0.03, 'B'), ('leukemia', 18, 19, 20, 0.03, 'B'),
        ('lung',      21, 22, 23, 0.15, 'B'), ('lymphoma',   24, 25, 26, 0.04, 'B'), ('melanoma', 27, 28, 29, 0.08, 'B'),
        ('nonmelanoma',30, 31, 32, 0.06, 'B'), ('oral',      33, 34, 35, 0.02, 'B'), ('ovarian',  36, 37, 38, 0.05, 'F'),
        ('pancreatic',39, 40, 41, 0.04, 'B'), ('prostate',   42, 43, 44, 0.25, 'M'), ('stomach',  45, 46, 47, 0.03, 'B'),
        ('testicular',48, 49, 50, 0.02, 'M'), ('thyroid',    51, 52, 53, 0.04, 'B'), ('other',    54, 56, 57, 0.03, 'B'),
        ('unknown',   58, NULL, 59, 0.03, 'B')
      ) v(name, o_flag, o_multi, o_age, rate, sex)
    LOOP
      rate := site.rate;
      IF site.sex = 'F' AND p_gender <> 'Female' THEN rate := CASE WHEN site.name = 'breast' THEN 0.01 ELSE 0 END; END IF;
      IF site.sex = 'M' AND p_gender <> 'Male'   THEN rate := 0; END IF;
      IF random() < rate THEN
        drew := true;
        PERFORM pg_temp.syn_answer(p_rid, p_step, p_inst, 2, 16, site.o_flag, 'true', p_ts);
        IF site.o_multi IS NOT NULL THEN
          PERFORM pg_temp.syn_answer(p_rid, p_step, p_inst, 2, 16, site.o_multi,
                  CASE WHEN random() < 0.12 THEN 'TRUE' ELSE 'FALSE' END, p_ts);
        END IF;
        dx := GREATEST(20, LEAST(p_age, 25 + floor(random()*60)::int));
        IF random() > 0.07 THEN PERFORM pg_temp.syn_answer(p_rid, p_step, p_inst, 2, 16, site.o_age, dx::text, p_ts); END IF;
        IF site.name = 'breast' THEN
          PERFORM pg_temp.syn_answer(p_rid, p_step, p_inst, 2, 16, 8,
                  (ARRAY['Yes','No','No','No','unknown'])[1 + floor(random()*5)::int], p_ts);
        END IF;
        IF site.name = 'other' THEN
          PERFORM pg_temp.syn_answer(p_rid, p_step, p_inst, 2, 16, 55,
                  (ARRAY['Sarcoma','Brain tumor','Liver cancer','Bone cancer','Esophageal cancer'])[1 + floor(random()*5)::int], p_ts);
        END IF;
      END IF;
    END LOOP;
  END LOOP;
END $$;

-- Respondents
DO $$
DECLARE
  n int := current_setting('syn.n')::int;
  i int; rid int; code text; created timestamptz; first timestamptz; fin timestamptz;
  p_gender text; p_age int; n_child int; n_sib int; n_msib int; n_fsib int; k int; r numeric;
BEGIN
  FOR i IN 1..n LOOP
    rid := nextval('survey.respondents_seq');
    code := 'SYN' || upper(substr(md5(rid::text), 1, 6));
    created := timestamptz '2021-01-01' + (random() * 1700)::int * interval '1 day' + (random()*86400)::int * interval '1 second';
    first := created + (60 + (random()*7*86400)::int) * interval '1 second';
    fin := first + (300 + (random()*3300)::int) * interval '1 second';

    INSERT INTO survey.respondents (id, survey_id, access_code, active, logins, created_dt, first_access_dt, finalized_dt)
    VALUES (rid, 1, code, false, 1 + floor(random()*3)::int, created, first, fin);

    INSERT INTO survey.subjects (id, xid, firstname, lastname, dob, email, department_id, survey_id, respondent_id, created_dt)
    VALUES (nextval('survey.subjects_seq'), 'SYN-' || lpad(rid::text, 6, '0'),
            (ARRAY['Ava','Liam','Noah','Mia','Ella','Owen','Zoe','Eli','Ivy','Max'])[1 + floor(random()*10)::int],
            (ARRAY['Rivera','Nguyen','Okafor','Schmidt','Patel','Kowalski','Haddad','Larsen','Moreau','Tanaka'])[1 + floor(random()*10)::int],
            date '1945-01-01' + (random()*20000)::int, 'syn' || i || '@example.invalid', 1, 1, rid, created);

    -- Proband (step 3 / section durable 5)
    r := random();
    p_gender := CASE WHEN r < 0.55 THEN 'Female' WHEN r < 0.99 THEN 'Male' ELSE 'Other' END;
    p_age := 25 + floor(random()*56)::int;
    PERFORM pg_temp.syn_answer(rid, 3, 0, 1, 5, 1, p_age::text, fin);                    -- Age (+ Vital Status=Alive constant)
    PERFORM pg_temp.syn_answer(rid, 3, 0, 1, 5, 7, p_gender, fin);                       -- Gender
    IF random() > 0.05 THEN PERFORM pg_temp.syn_answer(rid, 3, 0, 1, 5, 8,
        (ARRAY['No','No','No','No','No','No','Maternal','Paternal','Both Parents','Don''t Know'])[1 + floor(random()*10)::int], fin); END IF;
    IF random() > 0.05 THEN PERFORM pg_temp.syn_answer(rid, 3, 0, 1, 5, 14,
        (ARRAY['White','White','White','White','White','White','Black','Asian','Middle Eastern','Native American','Pacific Islander','Other'])[1 + floor(random()*12)::int], fin); END IF;
    IF random() > 0.05 THEN PERFORM pg_temp.syn_answer(rid, 3, 0, 1, 5, 16,
        CASE WHEN random() < 0.15 THEN 'LatinX' ELSE 'Not LatinX' END, fin); END IF;
    -- Proband cancers: step 4, Cancers section only
    IF random() < 0.35 THEN PERFORM pg_temp.syn_cancers(rid, 4, 0, p_gender, p_age, fin); END IF;

    n_child := floor(random()*5)::int; n_sib := floor(random()*5)::int;
    n_msib := floor(random()*5)::int;  n_fsib := floor(random()*5)::int;

    FOR k IN 1..n_child LOOP PERFORM pg_temp.syn_relative(rid, 5,  k, 8,  'child',     NULL,     1, fin); END LOOP;
    FOR k IN 1..n_sib   LOOP PERFORM pg_temp.syn_relative(rid, 6,  k, 9,  'sibling',   NULL,     0, fin); END LOOP;
    PERFORM pg_temp.syn_relative(rid, 7,  0, 6,  'parent', 'Female', -1, fin);   -- Mother
    PERFORM pg_temp.syn_relative(rid, 8,  0, 10, 'grand',  'Female', -2, fin);   -- Maternal Grandmother
    PERFORM pg_temp.syn_relative(rid, 9,  0, 11, 'grand',  'Male',   -2, fin);   -- Maternal Grandfather
    FOR k IN 1..n_msib  LOOP PERFORM pg_temp.syn_relative(rid, 10, k, 12, 'auntuncle', NULL,    -1, fin); END LOOP;
    PERFORM pg_temp.syn_relative(rid, 11, 0, 7,  'parent', 'Male',   -1, fin);   -- Father
    PERFORM pg_temp.syn_relative(rid, 12, 0, 13, 'grand',  'Female', -2, fin);   -- Paternal Grandmother
    PERFORM pg_temp.syn_relative(rid, 13, 0, 14, 'grand',  'Male',   -2, fin);   -- Paternal Grandfather
    FOR k IN 1..n_fsib  LOOP PERFORM pg_temp.syn_relative(rid, 14, k, 15, 'auntuncle', NULL,    -1, fin); END LOOP;
  END LOOP;
END $$;

SELECT count(*) AS synthetic_respondents FROM survey.respondents WHERE access_code LIKE 'SYN%';
SELECT count(*) AS synthetic_answers FROM survey.answers a JOIN survey.respondents r ON r.id = a.respondent_id WHERE r.access_code LIKE 'SYN%';
