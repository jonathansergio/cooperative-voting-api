package com.example.voting.session;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface VotingSessionRepository extends JpaRepository<VotingSession, Long> {

    Optional<VotingSession> findByTopicId(Long topicId);
}
