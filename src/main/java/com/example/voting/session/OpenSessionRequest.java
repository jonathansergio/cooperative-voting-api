package com.example.voting.session;

import jakarta.validation.constraints.Positive;

/** Duration is optional; leaving it out opens the session for the default of one minute. */
record OpenSessionRequest(@Positive Integer durationMinutes) {}
