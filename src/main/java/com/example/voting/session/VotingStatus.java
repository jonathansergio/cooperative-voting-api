package com.example.voting.session;

/**
 * What other domains are allowed to know about a topic's voting session. Keeping the answer to this
 * narrow question public, instead of the session itself, is what lets the vote domain ask without
 * reaching into this one.
 */
public enum VotingStatus {
    NOT_OPENED,
    OPEN,
    CLOSED
}
