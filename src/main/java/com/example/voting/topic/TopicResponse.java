package com.example.voting.topic;

import java.time.Instant;

record TopicResponse(Long id, String title, String description, Instant createdAt) {

    static TopicResponse from(Topic topic) {
        return new TopicResponse(topic.id(), topic.title(), topic.description(), topic.createdAt());
    }
}
