package com.brasper.platform.accounting.application.commandservices;

import com.brasper.platform.shared.application.result.CommandFailure;

public enum TaxCommandFailure implements CommandFailure {
    DUPLICATE_EFFECTIVE_DATE("accounting.error.duplicate-effective-date"),
    DUPLICATE_RETURN("accounting.error.duplicate-tax-return"),
    TAX_RETURN_NOT_FOUND("accounting.error.tax-return-not-found"),
    ALREADY_PAID("accounting.error.tax-return-already-paid"),
    BANK_ACCOUNT_NOT_FOUND("accounting.error.tax-bank-account-not-found"),
    ACCOUNTS_NOT_CONFIGURED("accounting.error.tax-accounts-not-configured");

    private final String messageKey;

    TaxCommandFailure(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }
}
