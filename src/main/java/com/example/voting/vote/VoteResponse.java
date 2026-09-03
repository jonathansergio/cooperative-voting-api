package com.example.voting.vote;

import java.time.Instant;

record VoteResponse(Long id, Long topicId, String memberId, Choice choice, Instant castAt) {

    static VoteResponse from(Vote vote) {
        return new VoteResponse(vote.id(), vote.topicId(), vote.memberId(), vote.choice(), vote.castAt());
    }
}
