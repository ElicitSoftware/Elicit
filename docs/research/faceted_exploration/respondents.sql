-- Respondents: one row per respondent, from fact_respondents_view + dim_date. Tabs = first 6 columns.
select r.status                        as "Status",
       case when f.finalized_key > 19700101 then fd.calendaryear::int end as "Finalized year",
       fd.monthname                    as "Finalized month",
       fd.daynameofweek                as "Finalized weekday",
       r.logins                        as "Logins",
       case when r.duration < interval '15 minutes' then 'under 15 min'
            when r.duration < interval '30 minutes' then '15-30 min'
            when r.duration < interval '60 minutes' then '30-60 min'
            else 'over 1 hour' end     as "Duration",
       r.active                        as "Active",
       r.created                       as "Created",
       r.first_access                  as "First access",
       r.finalized                     as "Finalized",
       r.id                            as "Respondent"
from surveyreport.fact_respondents_view r
join surveyreport.fact_respondents f on f.id = r.id
join surveyreport.dim_date fd on fd.datekey = f.finalized_key
order by r.id
