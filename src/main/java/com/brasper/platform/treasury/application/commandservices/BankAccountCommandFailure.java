package com.brasper.platform.treasury.application.commandservices;

import com.brasper.platform.shared.application.result.CommandFailure;

public enum BankAccountCommandFailure implements CommandFailure {
    DUPLICATE_ACCOUNT_NUMBER("treasury.error.duplicate-account-number"),
    BANK_ACCOUNT_NOT_FOUND("treasury.error.bank-account-not-found"),
    SAME_ACCOUNT("treasury.error.same-account"),
    CURRENCY_MISMATCH("treasury.error.currency-mismatch"),
    UNSUPPORTED_CURRENCY("treasury.error.unsupported-currency");

    private final String messageKey;

    BankAccountCommandFailure(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }
}
