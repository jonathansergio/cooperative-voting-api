package com.example.voting.vote;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record CastVoteRequest(@NotBlank @Size(max = 64) String memberId, @NotNull Choice choice) {}
