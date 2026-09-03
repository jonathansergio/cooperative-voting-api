package com.example.voting.session;

import org.springframework.data.jpa.repository.JpaRepository;

interface VotingSessionRepository extends JpaRepository<VotingSession, Long> {}
