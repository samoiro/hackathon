-- 배치 기준일. 원자재 가격은 기준일 전날 하루를 받는다. 지금까지는 job_parameters.runDate에 넣었다
alter table batch_job add column target_date date;

update batch_job
set target_date = coalesce((job_parameters ->> 'runDate')::date, (created_datetime at time zone 'Asia/Seoul')::date),
    job_parameters = job_parameters - 'runDate';

alter table batch_job alter column target_date set not null;

comment on column batch_job.target_date is '기준일';
