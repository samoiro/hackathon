-- 에러 코드 목록과 API 에러 발생 이력. GlobalExceptionHandler가 응답할 때마다 error_log에 한 줄 남긴다.
-- error_code의 행은 Java ErrorCode enum과 같아야 한다 (ErrorLogMigrationTest가 확인).

create table error_code (
    code        text    primary key check (code ~ '^[A-Z_]+$'),
    http_status integer not null check (http_status between 400 and 599),
    message     text    not null check (btrim(message) <> '')
);

insert into error_code (code, http_status, message) values
    ('BAD_REQUEST',         400, '요청 값이 올바르지 않습니다.'),
    ('NOT_FOUND',           404, '요청한 대상을 찾을 수 없습니다.'),
    ('METHOD_NOT_ALLOWED',  405, '지원하지 않는 HTTP 메서드입니다.'),
    ('CONFLICT',            409, '요청이 현재 상태와 충돌합니다.'),
    ('TOO_MANY_REQUESTS',   429, '요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.'),
    ('INTERNAL_ERROR',      500, '서버 내부 오류가 발생했습니다.'),
    ('EXTERNAL_API_ERROR',  502, '외부 API 호출에 실패했습니다.'),
    ('SERVICE_UNAVAILABLE', 503, '서비스를 일시적으로 사용할 수 없습니다.'),
    ('GATEWAY_TIMEOUT',     504, '외부 API 응답 시간이 초과되었습니다.'),
    ('PUBLIC_API_KEY_MISSING',       503, '공공데이터 API 인증키가 설정되지 않았습니다.'),
    ('PUBLIC_API_CONNECTION_FAILED', 502, '공공데이터 API에 연결하지 못했습니다.'),
    ('PUBLIC_API_BAD_REQUEST',       400, '공공데이터 API가 요청 값을 거부했습니다.'),
    ('PUBLIC_API_ERROR',             502, '공공데이터 API가 오류를 응답했습니다.'),
    ('PUBLIC_API_INVALID_RESPONSE',  502, '공공데이터 API 응답이 비어 있거나 형식이 올바르지 않습니다.');

create table error_log (
    id               bigint generated always as identity primary key,
    error_code       text    not null references error_code (code),
    http_status      integer not null check (http_status between 400 and 599),
    http_method      text    not null,
    request_path     text    not null,
    message          text,
    exception_class  text    not null,
    stack_trace      text,
    created_datetime timestamptz(3) not null default current_timestamp(3)
);

-- 최근 에러 조회·오래된 로그 정리용
create index error_log_created_idx on error_log (created_datetime);

comment on table error_code is '에러 코드';
comment on table error_log is '에러 로그';
