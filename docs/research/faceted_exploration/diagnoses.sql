-- Cancer diagnoses: one row per relative x diagnosed site. Tabs = first 7 columns.
with rel as (
  select respondent_id, step as relative, step_instance,
         max(nullif(gender,''))              as gender,
         max(nullif(vital_status,''))        as vital_status,
         max(nullif(generation,''))::numeric as generation,
         max(nullif(triple_negative_breast_cancer,'')) as triple_negative,
         max(nullif(other_cancer_name,''))   as other_cancer_name,
         max(nullif(bladder_cancer,'')) as bladder_cancer, max(nullif(bladder_cancer_age,'')) as bladder_cancer_age, max(nullif(multiple_bladder_cancers,'')) as multiple_bladder_cancers,
         max(nullif(breast_cancer,'')) as breast_cancer, max(nullif(breast_cancer_age,'')) as breast_cancer_age, max(nullif(multiple_breast_cancers,'')) as multiple_breast_cancers,
         max(nullif(colon_or_rectal_cancer,'')) as colon_or_rectal_cancer, max(nullif(colon_or_rectal_cancer_age,'')) as colon_or_rectal_cancer_age, max(nullif(multiple_colon_or_rectal_cancers,'')) as multiple_colon_or_rectal_cancers,
         max(nullif(endometrial_or_uterine_cancer,'')) as endometrial_or_uterine_cancer, max(nullif(endometrial_or_uterine_cancer_age,'')) as endometrial_or_uterine_cancer_age, max(nullif(multiple_endometrial_or_uterine_cancers,'')) as multiple_endometrial_or_uterine_cancers,
         max(nullif(kidney_renal_cell_cancer,'')) as kidney_renal_cell_cancer, max(nullif(kidney_renal_cell_cancer_age,'')) as kidney_renal_cell_cancer_age, max(nullif(multiple_kidney_renal_cell_cancers,'')) as multiple_kidney_renal_cell_cancers,
         max(nullif(leukemia,'')) as leukemia, max(nullif(leukemia_age,'')) as leukemia_age, max(nullif(multiple_leukemias,'')) as multiple_leukemias,
         max(nullif(lung_cancer,'')) as lung_cancer, max(nullif(lung_cancer_age,'')) as lung_cancer_age, max(nullif(multiple_lung_cancers,'')) as multiple_lung_cancers,
         max(nullif(lymphoma,'')) as lymphoma, max(nullif(lymphoma_age,'')) as lymphoma_age, max(nullif(multiple_lymphomas,'')) as multiple_lymphomas,
         max(nullif(melanoma_skin_cancer,'')) as melanoma_skin_cancer, max(nullif(melanoma_skin_cancer_age,'')) as melanoma_skin_cancer_age, max(nullif(multiple_melanoma_skin_cancers,'')) as multiple_melanoma_skin_cancers,
         max(nullif(nonmelanoma_skin_cancer,'')) as nonmelanoma_skin_cancer, max(nullif(nonmelanoma_skin_cancer_age,'')) as nonmelanoma_skin_cancer_age, max(nullif(multiple_nonmelanoma_skin_cancers,'')) as multiple_nonmelanoma_skin_cancers,
         max(nullif(oral_cavity_or_throat_cancer,'')) as oral_cavity_or_throat_cancer, max(nullif(oral_cavity_or_throat_cancer_age,'')) as oral_cavity_or_throat_cancer_age, max(nullif(multiple_oral_cavity_or_throat_cancers,'')) as multiple_oral_cavity_or_throat_cancers,
         max(nullif(other_cancer,'')) as other_cancer, max(nullif(other_age,'')) as other_age, max(nullif(multiple_other_cancers,'')) as multiple_other_cancers,
         max(nullif(ovarian_cancer,'')) as ovarian_cancer, max(nullif(ovarian_cancer_age,'')) as ovarian_cancer_age, max(nullif(multiple_ovarian_cancers,'')) as multiple_ovarian_cancers,
         max(nullif(pancreatic_cancer,'')) as pancreatic_cancer, max(nullif(pancreatic_cancer_age,'')) as pancreatic_cancer_age, max(nullif(multiple_pancreatic_cancers,'')) as multiple_pancreatic_cancers,
         max(nullif(prostate_cancer,'')) as prostate_cancer, max(nullif(prostate_cancer_age,'')) as prostate_cancer_age, max(nullif(multiple_prostate_cancers,'')) as multiple_prostate_cancers,
         max(nullif(stomach_cancer,'')) as stomach_cancer, max(nullif(stomach_cancer_age,'')) as stomach_cancer_age, max(nullif(multiple_stomach_cancers,'')) as multiple_stomach_cancers,
         max(nullif(testicular_cancer,'')) as testicular_cancer, max(nullif(testicular_cancer_age,'')) as testicular_cancer_age, max(nullif(multiple_testicular_cancers,'')) as multiple_testicular_cancers,
         max(nullif(thyroid_cancer,'')) as thyroid_cancer, max(nullif(thyroid_cancer_age,'')) as thyroid_cancer_age, max(nullif(multiple_thyroid_cancers,'')) as multiple_thyroid_cancers,
         max(nullif(unknown_cancer,'')) as unknown_cancer, max(nullif(unknown_cancer_age,'')) as unknown_cancer_age
  from surveyreport.fact_sections_view
  where coalesce(step,'') not in ('Welcome','Demographics','Thank you')
  group by respondent_id, step, step_instance
)
select relative                        as "Relative",
       gender                          as "Gender",
       vital_status                    as "Vital status",
       generation                      as "Generation",
       u.site                          as "Cancer site",
       u.dx_age::numeric               as "Age at diagnosis",
       u.multiple                      as "Multiple",
       case when u.site = 'Breast Cancer' then triple_negative end as "Triple negative",
       case when u.site = 'Other Cancer'  then other_cancer_name end as "Other cancer name",
       respondent_id                   as "Respondent",
       step_instance                   as "Instance"
from rel, lateral (values
   ('Bladder Cancer', bladder_cancer, bladder_cancer_age, multiple_bladder_cancers),
   ('Breast Cancer', breast_cancer, breast_cancer_age, multiple_breast_cancers),
   ('Colon or Rectal Cancer', colon_or_rectal_cancer, colon_or_rectal_cancer_age, multiple_colon_or_rectal_cancers),
   ('Endometrial or Uterine Cancer', endometrial_or_uterine_cancer, endometrial_or_uterine_cancer_age, multiple_endometrial_or_uterine_cancers),
   ('Kidney renal cell Cancer', kidney_renal_cell_cancer, kidney_renal_cell_cancer_age, multiple_kidney_renal_cell_cancers),
   ('Leukemia', leukemia, leukemia_age, multiple_leukemias),
   ('Lung Cancer', lung_cancer, lung_cancer_age, multiple_lung_cancers),
   ('Lymphoma', lymphoma, lymphoma_age, multiple_lymphomas),
   ('Melanoma Skin Cancer', melanoma_skin_cancer, melanoma_skin_cancer_age, multiple_melanoma_skin_cancers),
   ('Non-Melanoma Skin Cancer', nonmelanoma_skin_cancer, nonmelanoma_skin_cancer_age, multiple_nonmelanoma_skin_cancers),
   ('Oral Cavity or Throat Cancer', oral_cavity_or_throat_cancer, oral_cavity_or_throat_cancer_age, multiple_oral_cavity_or_throat_cancers),
   ('Other Cancer', other_cancer, other_age, multiple_other_cancers),
   ('Ovarian Cancer', ovarian_cancer, ovarian_cancer_age, multiple_ovarian_cancers),
   ('Pancreatic Cancer', pancreatic_cancer, pancreatic_cancer_age, multiple_pancreatic_cancers),
   ('Prostate Cancer', prostate_cancer, prostate_cancer_age, multiple_prostate_cancers),
   ('Stomach Cancer', stomach_cancer, stomach_cancer_age, multiple_stomach_cancers),
   ('Testicular Cancer', testicular_cancer, testicular_cancer_age, multiple_testicular_cancers),
   ('Thyroid Cancer', thyroid_cancer, thyroid_cancer_age, multiple_thyroid_cancers),
   ('Unknown Cancer', unknown_cancer, unknown_cancer_age, NULL::text)
 ) as u(site, flag, dx_age, multiple)
where u.flag = 'true'
order by respondent_id, relative, step_instance, u.site
