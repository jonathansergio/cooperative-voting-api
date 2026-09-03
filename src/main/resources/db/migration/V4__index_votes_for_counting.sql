-- The result is counted with an aggregate query per side, never by loading the votes. This index
-- serves those counts directly, which is what keeps the tally cheap on a topic with hundreds of
-- thousands of votes.
create index votes_topic_choice_idx on votes (topic_id, choice);
