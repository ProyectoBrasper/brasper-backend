package com.brasper.platform.treasury.interfaces.rest.transform;

import com.brasper.platform.shared.interfaces.rest.transform.ProblemDetailAssembler;
import com.brasper.platform.treasury.application.commandservices.LoanCommandFailure;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

public final class LoanFailureResponseAssembler {

    private LoanFailureResponseAssembler() {
    }

    public static ResponseEntity<ProblemDetail> toResponse(LoanCommandFailure failure, MessageSource messages) {
        String key = failure.messageKey();
        return switch (failure) {
            case LOAN_NOT_FOUND, INSTALLMENT_NOT_FOUND, LOAN_ACCOUNT_NOT_FOUND ->
                    ProblemDetailAssembler.notFound(messages, key);
            case INSTALLMENT_ALREADY_PAID, INSTALLMENTS_EXCEED_PRINCIPAL -> ProblemDetailAssembler.conflict(messages, key);
            case LOAN_CURRENCY_MISMATCH -> ProblemDetailAssembler.badRequest(messages, key);
        };
    }
}
