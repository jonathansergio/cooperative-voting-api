package com.example.voting.topic;

import org.springframework.data.jpa.repository.JpaRepository;

interface TopicRepository extends JpaRepository<Topic, Long> {}
