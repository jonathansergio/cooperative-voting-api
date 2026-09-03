package com.example.voting.session;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class VotingSessionController {

    private final VotingSessionService sessions;

    VotingSessionController(VotingSessionService sessions) {
        this.sessions = sessions;
    }

    @PostMapping("/api/v1/topics/{topicId}/sessions")
    ResponseEntity<VotingSessionResponse> open(
            @PathVariable Long topicId, @Valid @RequestBody OpenSessionRequest request) {
        VotingSessionResponse session = sessions.open(topicId, request.durationMinutes());
        return ResponseEntity.created(URI.create("/api/v1/sessions/" + session.id()))
                .body(session);
    }

    @GetMapping("/api/v1/sessions/{id}")
    VotingSessionResponse find(@PathVariable Long id) {
        return sessions.find(id);
    }
}
