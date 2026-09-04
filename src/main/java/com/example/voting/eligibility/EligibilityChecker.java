package com.example.voting.eligibility;

/**
 * The domain's side of the member eligibility lookup. This is the only genuine external system in
 * the application, so it is the only place that gets a port and an adapter: the domain states what
 * it needs to know, and everything about how that is fetched lives in {@code integration}.
 */
public interface EligibilityChecker {

    EligibilityStatus statusOf(String memberId);
}
