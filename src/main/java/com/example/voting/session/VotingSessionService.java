package com.example.voting.session;

import com.example.voting.shared.errors.ConflictException;
import com.example.voting.shared.errors.NotFoundException;
import com.example.voting.topic.TopicRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VotingSessionService {

    private static final Duration DEFAULT_DURATION = Duration.ofMinutes(1);

    private final VotingSessionRepository sessions;
    private final TopicRepository topics;
    private final Clock clock;

    VotingSessionService(VotingSessionRepository sessions, TopicRepository topics, Clock clock) {
        this.sessions = sessions;
        this.topics = topics;
        this.clock = clock;
    }

    @Transactional
    VotingSessionResponse open(Long topicId, Integer durationMinutes) {
        if (!topics.existsById(topicId)) {
            throw new NotFoundException("Topic %d not found".formatted(topicId));
        }
        Instant now = clock.instant();
        VotingSession session = new VotingSession(topicId, now, durationOf(durationMinutes));
        try {
            // Uniqueness belongs to the database. Asking first and inserting afterwards would let two
            // concurrent requests both pass the check and open two sessions on the same topic.
            return VotingSessionResponse.of(sessions.saveAndFlush(session), now);
        } catch (DataIntegrityViolationException alreadyOpened) {
            throw new ConflictException("Topic %d already has a voting session".formatted(topicId));
        }
    }

    /** The topics accepting votes right now, answered in one query rather than one per topic. */
    @Transactional(readOnly = true)
    public List<Long> topicsAcceptingVotes() {
        return sessions.topicIdsAcceptingVotesAt(clock.instant());
    }

    /** Answers, for another domain, whether the topic is accepting votes right now. */
    @Transactional(readOnly = true)
    public VotingStatus statusFor(Long topicId) {
        return sessions.findByTopicId(topicId)
                .map(session -> session.isOpenAt(clock.instant()) ? VotingStatus.OPEN : VotingStatus.CLOSED)
                .orElse(VotingStatus.NOT_OPENED);
    }

    @Transactional(readOnly = true)
    VotingSessionResponse find(Long id) {
        return sessions.findById(id)
                .map(session -> VotingSessionResponse.of(session, clock.instant()))
                .orElseThrow(() -> new NotFoundException("Voting session %d not found".formatted(id)));
    }

    private Duration durationOf(Integer minutes) {
        return minutes == null ? DEFAULT_DURATION : Duration.ofMinutes(minutes);
    }
}
