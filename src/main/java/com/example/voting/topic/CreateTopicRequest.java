package com.example.voting.topic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateTopicRequest(@NotBlank @Size(max = 255) String title, String description) {}
