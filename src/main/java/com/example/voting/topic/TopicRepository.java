package com.example.voting.topic;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    @Query("select new com.example.voting.topic.TopicSummary(t.id, t.title, t.description) from Topic t "
            + "where t.id in :ids order by t.id")
    List<TopicSummary> summariesOf(@Param("ids") Collection<Long> ids);

    @Query("select new com.example.voting.topic.TopicSummary(t.id, t.title, t.description) from Topic t "
            + "where t.id = :id")
    Optional<TopicSummary> summaryOf(@Param("id") Long id);
}
