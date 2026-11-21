begin;

create table batch_job (
    id bigint generated always as identity primary key,
    job_name text not null check (btrim(job_name) <> ''),
    job_parameters jsonb not null default '{}'::jsonb check (jsonb_typeof(job_parameters) = 'object'),
    status text not null default 'PENDING' check (status in ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    retry_times integer not null default 0 check (retry_times >= 0),
    created_datetime timestamptz(0) not null default current_timestamp(0),
    updated_datetime timestamptz(0) not null default current_timestamp(0)
);

create table batch_job_history (
    id bigint generated always as identity primary key,
    batch_job_id bigint not null references batch_job(id) on delete restrict on update restrict,
    retry_times integer not null check (retry_times >= 0),
    status text not null default 'RUNNING' check (status in ('RUNNING', 'SUCCEEDED', 'FAILED')),
    started_datetime timestamptz(3) not null default current_timestamp(3),
    finished_datetime timestamptz(3),
    running_time bigint generated always as (
        (extract(epoch from (finished_datetime - started_datetime)) * 1000)::bigint
    ) stored,
    error_code text,
    error_message text,
    read_count bigint not null default 0 check (read_count >= 0),
    write_count bigint not null default 0 check (write_count >= 0),
    skip_count bigint not null default 0 check (skip_count >= 0),
    created_datetime timestamptz(0) not null default current_timestamp(0),
    updated_datetime timestamptz(0) not null default current_timestamp(0),
    unique (batch_job_id, retry_times),
    check (finished_datetime is null or finished_datetime >= started_datetime),
    check ((status = 'RUNNING' and finished_datetime is null)
        or (status in ('SUCCEEDED', 'FAILED') and finished_datetime is not null))
);

create unique index batch_job_history_running_idx on batch_job_history (batch_job_id) where status = 'RUNNING';

create function batch_job_set_timestamps() returns trigger language plpgsql as $$
begin
    new.created_datetime := old.created_datetime;
    new.updated_datetime := old.updated_datetime;
    if (to_jsonb(new) - 'running_time') is distinct from (to_jsonb(old) - 'running_time') then
        new.updated_datetime := clock_timestamp()::timestamptz(0);
    end if;
    return new;
end;
$$;

create trigger batch_job_timestamps before update on batch_job
for each row execute function batch_job_set_timestamps();
create trigger batch_job_history_timestamps before update on batch_job_history
for each row execute function batch_job_set_timestamps();

comment on table batch_job is '수집 작업과 현재 재시도 횟수. 정기 수집 회차마다 새 작업 생성';
comment on column batch_job.retry_times is '최초 실행 0, 재시도 1부터. 최대 재시도는 application.yml의 batch.max-retry-times로 제어';
comment on table batch_job_history is '작업별 최초 실행·재시도 이력. batch_job과 1:N';
comment on column batch_job_history.retry_times is '해당 실행의 재시도 차수. 부모 횟수 증가와 실행 이력 생성은 애플리케이션에서 같은 트랜잭션으로 처리';
comment on column batch_job_history.running_time is '종료 - 시작으로 자동 계산한 소요시간(ms). 실행 중에는 NULL';
comment on column batch_job_history.error_message is '인증키·접속정보 등 비밀값을 제거한 오류 설명';

commit;
