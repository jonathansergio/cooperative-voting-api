package com.example.voting.vote;

import org.springframework.data.jpa.repository.JpaRepository;

interface VoteRepository extends JpaRepository<Vote, Long> {

    long countByTopicId(Long topicId);
}
