package com.brasper.platform.staff.interfaces.rest.transform;

import com.brasper.platform.shared.interfaces.rest.transform.ProblemDetailAssembler;
import com.brasper.platform.staff.application.commandservices.FixedCostCommandFailure;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

public final class FixedCostFailureResponseAssembler {
    private FixedCostFailureResponseAssembler() {}
    public static ResponseEntity<ProblemDetail> toResponse(FixedCostCommandFailure failure, MessageSource messages) {
        String key = failure.messageKey();
        return switch (failure) {
            case DUPLICATE_CODE, FIXED_COST_INACTIVE -> ProblemDetailAssembler.conflict(messages, key);
            case EMPLOYEE_REQUIRED -> ProblemDetailAssembler.badRequest(messages, key);
            case UNSUPPORTED_CURRENCY -> ProblemDetailAssembler.unprocessable(messages, key);
            case EMPLOYEE_NOT_FOUND, FIXED_COST_NOT_FOUND, BANK_ACCOUNT_NOT_FOUND -> ProblemDetailAssembler.notFound(messages, key);
        };
    }
}
