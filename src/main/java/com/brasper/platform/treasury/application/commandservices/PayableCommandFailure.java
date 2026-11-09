package com.brasper.platform.treasury.application.commandservices;

import com.brasper.platform.shared.application.result.CommandFailure;

public enum PayableCommandFailure implements CommandFailure {
    PAYABLE_NOT_FOUND("treasury.error.payable-not-found"),
    PAYABLE_ALREADY_PAID("treasury.error.payable-already-paid"),
    PAYMENT_EXCEEDS_BALANCE("treasury.error.payment-exceeds-balance"),
    PAYMENT_ACCOUNT_NOT_FOUND("treasury.error.payment-account-not-found"),
    PAYMENT_CURRENCY_MISMATCH("treasury.error.payment-currency-mismatch"),
    INVALID_PAYMENT_AMOUNT("treasury.error.invalid-payment-amount"),
    UNSUPPORTED_CURRENCY("treasury.error.unsupported-currency");

    private final String messageKey;

    PayableCommandFailure(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }
}
