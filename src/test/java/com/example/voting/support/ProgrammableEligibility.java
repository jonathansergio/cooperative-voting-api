package com.example.voting.support;

import com.example.voting.eligibility.EligibilityChecker;
import com.example.voting.eligibility.EligibilityStatus;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stands in for the external eligibility service so a test can say what the answer is. Members that
 * were not given an answer are allowed to vote, which keeps tests about other things short.
 */
public final class ProgrammableEligibility implements EligibilityChecker, Resettable {

    private final Map<String, EligibilityStatus> answers = new ConcurrentHashMap<>();

    public void answer(String memberId, EligibilityStatus status) {
        answers.put(memberId, status);
    }

    @Override
    public void reset() {
        answers.clear();
    }

    @Override
    public EligibilityStatus statusOf(String memberId) {
        return answers.getOrDefault(memberId, EligibilityStatus.ABLE_TO_VOTE);
    }
}
