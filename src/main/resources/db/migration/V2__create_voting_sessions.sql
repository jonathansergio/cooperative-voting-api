create table voting_sessions (
    id bigint generated always as identity primary key,
    topic_id bigint not null references topics (id),
    opened_at timestamptz not null,
    closes_at timestamptz not null,
    constraint voting_sessions_one_per_topic unique (topic_id)
);
