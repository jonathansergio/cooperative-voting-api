package com.example.voting.topic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "topics")
class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Topic() {}

    Topic(String title, String description, Instant createdAt) {
        this.title = title;
        this.description = description;
        this.createdAt = createdAt;
    }

    Long id() {
        return id;
    }

    String title() {
        return title;
    }

    String description() {
        return description;
    }

    Instant createdAt() {
        return createdAt;
    }
}
