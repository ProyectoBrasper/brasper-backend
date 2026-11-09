package com.brasper.platform.treasury.application.commandservices;

import com.brasper.platform.shared.application.result.CommandFailure;

public enum PartnerCommandFailure implements CommandFailure {
    PARTNER_NOT_FOUND("treasury.error.partner-not-found"),
    DUPLICATE_PARTNER("treasury.error.duplicate-partner"),
    INVALID_AMOUNT("treasury.error.invalid-contribution-amount"),
    CONTRIBUTION_ACCOUNT_NOT_FOUND("treasury.error.contribution-account-not-found"),
    UNSUPPORTED_CURRENCY("treasury.error.unsupported-currency");

    private final String messageKey;

    PartnerCommandFailure(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }
}
