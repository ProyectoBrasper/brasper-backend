package com.brasper.platform.staff.interfaces.rest.transform;

import com.brasper.platform.shared.interfaces.rest.transform.ProblemDetailAssembler;
import com.brasper.platform.staff.application.commandservices.EmployeeCommandFailure;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

public final class EmployeeFailureResponseAssembler {

    private EmployeeFailureResponseAssembler() {
    }

    public static ResponseEntity<ProblemDetail> toResponse(EmployeeCommandFailure failure, MessageSource messages) {
        String key = failure.messageKey();
        return switch (failure) {
            case EMPLOYEE_NOT_FOUND -> ProblemDetailAssembler.notFound(messages, key);
            case DUPLICATE_DNI, ALREADY_TERMINATED -> ProblemDetailAssembler.conflict(messages, key);
            case INVALID_TERMINATION_DATE -> ProblemDetailAssembler.badRequest(messages, key);
        };
    }
}
