package com.brasper.platform.expenses.application.commandservices;

import com.brasper.platform.shared.application.result.CommandFailure;

public enum ExpenseCommandFailure implements CommandFailure {
    PROJECT_NOT_FOUND("expenses.error.project-not-found"),
    PROJECT_CLOSED("expenses.error.project-closed"),
    SUPPLIER_NOT_FOUND("expenses.error.supplier-not-found"),
    BANK_ACCOUNT_REQUIRED("expenses.error.bank-account-required"),
    PARTNER_REQUIRED("expenses.error.partner-required"),
    BANK_ACCOUNT_NOT_FOUND("expenses.error.bank-account-not-found"),
    PARTNER_NOT_FOUND("expenses.error.partner-not-found"),
    EXPENSE_NOT_FOUND("expenses.error.expense-not-found"),
    ALREADY_ANNULLED("expenses.error.already-annulled");

    private final String messageKey;

    ExpenseCommandFailure(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }
}
