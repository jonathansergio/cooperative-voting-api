create table votes (
    id bigint generated always as identity primary key,
    topic_id bigint not null references topics (id),
    member_id varchar(64) not null,
    choice varchar(3) not null,
    cast_at timestamptz not null,
    -- One vote per member per topic. This is the only thing standing between the rule and two
    -- simultaneous requests from the same member; the application never checks before inserting.
    constraint votes_one_per_member_per_topic unique (topic_id, member_id)
);
