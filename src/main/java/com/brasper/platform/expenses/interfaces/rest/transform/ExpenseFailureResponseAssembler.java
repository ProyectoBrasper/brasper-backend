package com.brasper.platform.expenses.interfaces.rest.transform;

import com.brasper.platform.expenses.application.commandservices.ExpenseCommandFailure;
import com.brasper.platform.shared.interfaces.rest.transform.ProblemDetailAssembler;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

public final class ExpenseFailureResponseAssembler {

    private ExpenseFailureResponseAssembler() {
    }

    public static ResponseEntity<ProblemDetail> toResponse(ExpenseCommandFailure failure, MessageSource messages) {
        String key = failure.messageKey();
        return switch (failure) {
            case PROJECT_NOT_FOUND, SUPPLIER_NOT_FOUND, EXPENSE_NOT_FOUND, BANK_ACCOUNT_NOT_FOUND,
                    PARTNER_NOT_FOUND -> ProblemDetailAssembler.notFound(messages, key);
            case PROJECT_CLOSED, ALREADY_ANNULLED -> ProblemDetailAssembler.conflict(messages, key);
            case BANK_ACCOUNT_REQUIRED, PARTNER_REQUIRED -> ProblemDetailAssembler.badRequest(messages, key);
            case UNSUPPORTED_CURRENCY -> ProblemDetailAssembler.unprocessable(messages, key);
        };
    }
}
