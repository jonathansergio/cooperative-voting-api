package com.example.voting.support;

import com.example.voting.eligibility.EligibilityChecker;
import com.example.voting.eligibility.EligibilityStatus;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Stands in for the external eligibility service so a test can say what the answer is. Members that
 * were not given an answer are allowed to vote, which keeps tests about other things short.
 *
 * <p>It also remembers whether it was called with a database transaction open. The real service is a
 * network call that can take seconds, and a transaction open around it would hold a pooled
 * connection for all that time.
 */
public final class ProgrammableEligibility implements EligibilityChecker, Resettable {

    private final Map<String, EligibilityStatus> answers = new ConcurrentHashMap<>();
    private final AtomicReference<RuntimeException> failure = new AtomicReference<>();
    private volatile boolean lastCheckRanInsideTransaction;

    public void answer(String memberId, EligibilityStatus status) {
        answers.put(memberId, status);
    }

    /** Makes every following check blow up the way an unforeseen defect would. */
    public void failWith(RuntimeException unexpected) {
        failure.set(unexpected);
    }

    public boolean lastCheckRanInsideTransaction() {
        return lastCheckRanInsideTransaction;
    }

    @Override
    public void reset() {
        answers.clear();
        failure.set(null);
        lastCheckRanInsideTransaction = false;
    }

    @Override
    public EligibilityStatus statusOf(String memberId) {
        lastCheckRanInsideTransaction = TransactionSynchronizationManager.isActualTransactionActive();
        RuntimeException unexpected = failure.get();
        if (unexpected != null) {
            throw unexpected;
        }
        return answers.getOrDefault(memberId, EligibilityStatus.ABLE_TO_VOTE);
    }
}
