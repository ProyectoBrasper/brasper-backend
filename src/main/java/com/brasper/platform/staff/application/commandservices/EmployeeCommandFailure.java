package com.brasper.platform.staff.application.commandservices;

import com.brasper.platform.shared.application.result.CommandFailure;

public enum EmployeeCommandFailure implements CommandFailure {
    DUPLICATE_DNI("staff.error.duplicate-dni"),
    EMPLOYEE_NOT_FOUND("staff.error.employee-not-found"),
    ALREADY_TERMINATED("staff.error.already-terminated"),
    INVALID_TERMINATION_DATE("staff.error.invalid-termination-date");

    private final String messageKey;

    EmployeeCommandFailure(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }
}
