package com.brasper.platform.billing.application.commandservices;

import com.brasper.platform.shared.application.result.CommandFailure;

public enum InvoiceCommandFailure implements CommandFailure {
    DUPLICATE_NUMBER("billing.error.duplicate-number"),
    PROJECT_NOT_FOUND("billing.error.project-not-found"),
    PROJECT_NOT_BILLABLE("billing.error.project-not-billable"),
    INVOICE_NOT_FOUND("billing.error.invoice-not-found"),
    ALREADY_VOIDED("billing.error.already-voided"),
    HAS_PAYMENTS("billing.error.has-payments"),
    AMOUNT_EXCEEDS_BALANCE("billing.error.amount-exceeds-balance"),
    PAYMENT_CURRENCY_MISMATCH("billing.error.payment-currency-mismatch");

    private final String messageKey;

    InvoiceCommandFailure(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }
}
