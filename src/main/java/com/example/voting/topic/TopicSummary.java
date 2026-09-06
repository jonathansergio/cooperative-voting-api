package com.example.voting.topic;

/** What another domain is allowed to know about a topic. The entity itself stays in this package. */
public record TopicSummary(Long id, String title, String description) {}
