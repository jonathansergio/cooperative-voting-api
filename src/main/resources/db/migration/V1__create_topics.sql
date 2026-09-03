create table topics (
    id bigint generated always as identity primary key,
    title varchar(255) not null,
    description text,
    created_at timestamptz not null
);
