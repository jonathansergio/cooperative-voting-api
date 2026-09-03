package com.example.voting.vote;

import org.springframework.data.jpa.repository.JpaRepository;

interface VoteRepository extends JpaRepository<Vote, Long> {

    long countByTopicId(Long topicId);

    /** Counted in the database, so the tally never depends on how many votes a topic has. */
    long countByTopicIdAndChoice(Long topicId, Choice choice);
}
