package com.brasper.platform.accounting.application.commandservices;

import com.brasper.platform.shared.application.result.CommandFailure;

public enum TaxCommandFailure implements CommandFailure {
    DUPLICATE_EFFECTIVE_DATE("accounting.error.duplicate-effective-date");

    private final String messageKey;

    TaxCommandFailure(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }
}
