package com.example.voting.integration.userinfo;

import com.example.voting.eligibility.EligibilityStatus;

/** The upstream's payload. It stays inside this package; the domain only ever sees the enum. */
record UserInfoResponse(String status) {

    EligibilityStatus toStatus() {
        return "ABLE_TO_VOTE".equals(status) ? EligibilityStatus.ABLE_TO_VOTE : EligibilityStatus.UNABLE_TO_VOTE;
    }
}
