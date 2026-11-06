package com.brasper.platform.expenses.application.commandservices;

import com.brasper.platform.shared.application.result.CommandFailure;

public enum SupplierCommandFailure implements CommandFailure {
    DUPLICATE_RUC("expenses.error.duplicate-ruc"),
    RUC_REQUIRED_FOR_COMPANY("expenses.error.ruc-required-for-company"),
    SUPPLIER_NOT_FOUND("expenses.error.supplier-not-found");

    private final String messageKey;

    SupplierCommandFailure(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }
}
