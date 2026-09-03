package com.example.voting.vote;

record VoteResultResponse(Long topicId, long yes, long no, long total, Outcome outcome, boolean votingOpen) {

    static VoteResultResponse of(Long topicId, long yes, long no, boolean votingOpen) {
        return new VoteResultResponse(topicId, yes, no, yes + no, Outcome.of(yes, no), votingOpen);
    }
}
