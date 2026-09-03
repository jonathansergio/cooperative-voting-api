package com.example.voting.vote;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "votes")
class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Choice choice;

    @Column(name = "cast_at", nullable = false)
    private Instant castAt;

    protected Vote() {}

    Vote(Long topicId, String memberId, Choice choice, Instant castAt) {
        this.topicId = topicId;
        this.memberId = memberId;
        this.choice = choice;
        this.castAt = castAt;
    }

    Long id() {
        return id;
    }

    Long topicId() {
        return topicId;
    }

    String memberId() {
        return memberId;
    }

    Choice choice() {
        return choice;
    }

    Instant castAt() {
        return castAt;
    }
}
