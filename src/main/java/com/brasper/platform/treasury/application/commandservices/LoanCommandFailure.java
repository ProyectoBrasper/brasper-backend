package com.brasper.platform.treasury.application.commandservices;

import com.brasper.platform.shared.application.result.CommandFailure;

public enum LoanCommandFailure implements CommandFailure {
    LOAN_NOT_FOUND("treasury.error.loan-not-found"),
    INSTALLMENT_NOT_FOUND("treasury.error.installment-not-found"),
    INSTALLMENT_ALREADY_PAID("treasury.error.installment-already-paid"),
    INSTALLMENTS_EXCEED_PRINCIPAL("treasury.error.installments-exceed-principal"),
    LOAN_ACCOUNT_NOT_FOUND("treasury.error.loan-account-not-found"),
    LOAN_CURRENCY_MISMATCH("treasury.error.loan-currency-mismatch");

    private final String messageKey;

    LoanCommandFailure(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }
}
