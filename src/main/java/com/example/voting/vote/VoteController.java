package com.example.voting.vote;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class VoteController {

    private final VoteService votes;

    VoteController(VoteService votes) {
        this.votes = votes;
    }

    @PostMapping("/api/v1/topics/{topicId}/votes")
    @ResponseStatus(HttpStatus.CREATED)
    VoteResponse cast(@PathVariable long topicId, @Valid @RequestBody CastVoteRequest request) {
        return votes.cast(topicId, request.memberId(), request.choice());
    }
}
