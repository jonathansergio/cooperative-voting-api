package com.example.voting.vote;

import com.example.voting.session.VotingSessionService;
import com.example.voting.shared.errors.ConflictException;
import com.example.voting.shared.errors.NotFoundException;
import com.example.voting.shared.errors.UnprocessableException;
import com.example.voting.topic.TopicRepository;
import java.time.Clock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class VoteService {

    private final VoteRepository votes;
    private final TopicRepository topics;
    private final VotingSessionService sessions;
    private final Clock clock;

    VoteService(VoteRepository votes, TopicRepository topics, VotingSessionService sessions, Clock clock) {
        this.votes = votes;
        this.topics = topics;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Transactional
    VoteResponse cast(long topicId, String memberId, Choice choice) {
        if (!topics.existsById(topicId)) {
            throw new NotFoundException("Topic %d not found".formatted(topicId));
        }
        requireAnOpenSession(topicId);
        try {
            // A member votes at most once per topic, and the unique index is what enforces it.
            // Reading the table first and inserting afterwards would let two simultaneous requests
            // from the same member both find nothing and both insert.
            return VoteResponse.from(votes.saveAndFlush(new Vote(topicId, memberId, choice, clock.instant())));
        } catch (DataIntegrityViolationException alreadyVoted) {
            throw new ConflictException("Member %s has already voted on topic %d".formatted(memberId, topicId));
        }
    }

    private void requireAnOpenSession(long topicId) {
        switch (sessions.statusFor(topicId)) {
            case NOT_OPENED -> throw new UnprocessableException("Topic %d has no voting session".formatted(topicId));
            case CLOSED ->
                throw new UnprocessableException("The voting session for topic %d is closed".formatted(topicId));
            case OPEN -> {}
        }
    }
}
