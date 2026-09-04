package com.example.voting.vote;

import com.example.voting.eligibility.EligibilityChecker;
import com.example.voting.session.VotingSessionService;
import com.example.voting.session.VotingStatus;
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
    private final EligibilityChecker eligibility;
    private final Clock clock;

    VoteService(
            VoteRepository votes,
            TopicRepository topics,
            VotingSessionService sessions,
            EligibilityChecker eligibility,
            Clock clock) {
        this.votes = votes;
        this.topics = topics;
        this.sessions = sessions;
        this.eligibility = eligibility;
        this.clock = clock;
    }

    @Transactional
    VoteResponse cast(long topicId, String memberId, Choice choice) {
        requireTheTopicExists(topicId);
        requireAnOpenSession(topicId);
        requireAnEligibleMember(memberId);
        try {
            // A member votes at most once per topic, and the unique index is what enforces it.
            // Reading the table first and inserting afterwards would let two simultaneous requests
            // from the same member both find nothing and both insert.
            return VoteResponse.from(votes.saveAndFlush(new Vote(topicId, memberId, choice, clock.instant())));
        } catch (DataIntegrityViolationException alreadyVoted) {
            throw new ConflictException("Member %s has already voted on topic %d".formatted(memberId, topicId));
        }
    }

    /**
     * The result is available while the voting is still running, flagged as such, and stays
     * available once it closes. What it is never available for is a topic nobody has put to vote.
     */
    @Transactional(readOnly = true)
    VoteResultResponse resultFor(long topicId) {
        requireTheTopicExists(topicId);
        VotingStatus status = sessions.statusFor(topicId);
        if (status == VotingStatus.NOT_OPENED) {
            throw new UnprocessableException("Topic %d has no voting session".formatted(topicId));
        }
        return VoteResultResponse.of(
                topicId,
                votes.countByTopicIdAndChoice(topicId, Choice.YES),
                votes.countByTopicIdAndChoice(topicId, Choice.NO),
                status == VotingStatus.OPEN);
    }

    private void requireAnEligibleMember(String memberId) {
        switch (eligibility.statusOf(memberId)) {
            case UNABLE_TO_VOTE ->
                throw new UnprocessableException("Member %s is not allowed to vote".formatted(memberId));
            case UNKNOWN_MEMBER ->
                throw new UnprocessableException("Member %s is not a known member".formatted(memberId));
            case ABLE_TO_VOTE -> {}
        }
    }

    private void requireTheTopicExists(long topicId) {
        if (!topics.existsById(topicId)) {
            throw new NotFoundException("Topic %d not found".formatted(topicId));
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
