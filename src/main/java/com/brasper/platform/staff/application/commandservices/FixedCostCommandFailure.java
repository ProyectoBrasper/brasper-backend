package com.brasper.platform.staff.application.commandservices;

import com.brasper.platform.shared.application.result.CommandFailure;

public enum FixedCostCommandFailure implements CommandFailure {
    DUPLICATE_CODE("staff.error.duplicate-cost-code"),
    EMPLOYEE_REQUIRED("staff.error.employee-required"),
    EMPLOYEE_NOT_FOUND("staff.error.employee-not-found"),
    FIXED_COST_NOT_FOUND("staff.error.fixed-cost-not-found"),
    FIXED_COST_INACTIVE("staff.error.fixed-cost-inactive"),
    BANK_ACCOUNT_NOT_FOUND("staff.error.bank-account-not-found");

    private final String messageKey;

    FixedCostCommandFailure(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }
}
