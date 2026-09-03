package com.example.voting.topic;

import com.example.voting.shared.errors.NotFoundException;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Clock;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/topics")
class TopicController {

    private final TopicRepository topics;
    private final Clock clock;

    TopicController(TopicRepository topics, Clock clock) {
        this.topics = topics;
        this.clock = clock;
    }

    @PostMapping
    ResponseEntity<TopicResponse> register(@Valid @RequestBody CreateTopicRequest request) {
        Topic topic = topics.save(new Topic(request.title(), request.description(), clock.instant()));
        return ResponseEntity.created(URI.create("/api/v1/topics/" + topic.id()))
                .body(TopicResponse.from(topic));
    }

    @GetMapping("/{id}")
    TopicResponse find(@PathVariable Long id) {
        return topics.findById(id)
                .map(TopicResponse::from)
                .orElseThrow(() -> new NotFoundException("Topic %d not found".formatted(id)));
    }
}
