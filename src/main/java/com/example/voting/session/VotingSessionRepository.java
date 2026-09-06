package com.example.voting.session;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface VotingSessionRepository extends JpaRepository<VotingSession, Long> {

    Optional<VotingSession> findByTopicId(Long topicId);

    @Query("select s.topicId from VotingSession s where s.closesAt > :moment order by s.topicId")
    List<Long> topicIdsAcceptingVotesAt(@Param("moment") Instant moment);
}
