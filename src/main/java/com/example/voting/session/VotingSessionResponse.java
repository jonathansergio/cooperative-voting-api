package com.example.voting.session;

import java.time.Instant;

record VotingSessionResponse(Long id, Long topicId, Instant openedAt, Instant closesAt, boolean open) {

    static VotingSessionResponse of(VotingSession session, Instant now) {
        return new VotingSessionResponse(
                session.id(), session.topicId(), session.openedAt(), session.closesAt(), session.isOpenAt(now));
    }
}
