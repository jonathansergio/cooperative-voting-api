package com.example.voting.vote;

/** What the counted votes say. A topic with no votes at all counts as tied. */
enum Outcome {
    APPROVED,
    REJECTED,
    TIED;

    static Outcome of(long yes, long no) {
        if (yes > no) {
            return APPROVED;
        }
        return yes < no ? REJECTED : TIED;
    }
}
