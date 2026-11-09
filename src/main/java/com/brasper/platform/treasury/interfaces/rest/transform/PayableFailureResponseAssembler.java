package com.brasper.platform.treasury.interfaces.rest.transform;

import com.brasper.platform.shared.interfaces.rest.transform.ProblemDetailAssembler;
import com.brasper.platform.treasury.application.commandservices.PayableCommandFailure;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

public final class PayableFailureResponseAssembler {

    private PayableFailureResponseAssembler() {
    }

    public static ResponseEntity<ProblemDetail> toResponse(PayableCommandFailure failure, MessageSource messages) {
        String key = failure.messageKey();
        return switch (failure) {
            case PAYABLE_NOT_FOUND, PAYMENT_ACCOUNT_NOT_FOUND -> ProblemDetailAssembler.notFound(messages, key);
            case PAYABLE_ALREADY_PAID, PAYMENT_EXCEEDS_BALANCE -> ProblemDetailAssembler.conflict(messages, key);
            case PAYMENT_CURRENCY_MISMATCH, INVALID_PAYMENT_AMOUNT -> ProblemDetailAssembler.badRequest(messages, key);
            case UNSUPPORTED_CURRENCY -> ProblemDetailAssembler.unprocessable(messages, key);
        };
    }
}
