package com.brasper.platform.accounting.application.commandservices;

import com.brasper.platform.shared.application.result.CommandFailure;

public enum LedgerCommandFailure implements CommandFailure {
    DUPLICATE_ACCOUNT_CODE("accounting.error.duplicate-account-code"),
    ACCOUNT_NOT_FOUND("accounting.error.ledger-account-not-found"),
    ACCOUNT_INACTIVE("accounting.error.ledger-account-inactive"),
    INVALID_ENTRY("accounting.error.invalid-journal-entry"),
    DUPLICATE_SOURCE("accounting.error.duplicate-entry-source"),
    ENTRY_NOT_FOUND("accounting.error.journal-entry-not-found"),
    ALREADY_REVERSED("accounting.error.entry-already-reversed");

    private final String messageKey;

    LedgerCommandFailure(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }
}
