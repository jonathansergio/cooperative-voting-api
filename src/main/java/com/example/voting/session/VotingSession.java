package com.example.voting.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "voting_sessions")
class VotingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closes_at", nullable = false)
    private Instant closesAt;

    protected VotingSession() {}

    VotingSession(Long topicId, Instant openedAt, Duration duration) {
        this.topicId = topicId;
        this.openedAt = openedAt;
        this.closesAt = openedAt.plus(duration);
    }

    /**
     * Whether the session accepts votes at the given moment. The state is derived from the closing
     * time on every read, never stored, so no scheduled job can leave it stale and a restart cannot
     * lose it.
     */
    boolean isOpenAt(Instant moment) {
        return moment.isBefore(closesAt);
    }

    Long id() {
        return id;
    }

    Long topicId() {
        return topicId;
    }

    Instant openedAt() {
        return openedAt;
    }

    Instant closesAt() {
        return closesAt;
    }
}
