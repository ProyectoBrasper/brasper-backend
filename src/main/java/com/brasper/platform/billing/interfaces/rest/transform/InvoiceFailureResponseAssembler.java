package com.brasper.platform.billing.interfaces.rest.transform;

import com.brasper.platform.billing.application.commandservices.InvoiceCommandFailure;
import com.brasper.platform.shared.interfaces.rest.transform.ProblemDetailAssembler;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

public final class InvoiceFailureResponseAssembler {

    private InvoiceFailureResponseAssembler() {
    }

    public static ResponseEntity<ProblemDetail> toResponse(InvoiceCommandFailure failure, MessageSource messages) {
        String key = failure.messageKey();
        return switch (failure) {
            case DUPLICATE_NUMBER, PROJECT_NOT_BILLABLE, ALREADY_VOIDED, HAS_PAYMENTS ->
                    ProblemDetailAssembler.conflict(messages, key);
            case AMOUNT_EXCEEDS_BALANCE, PAYMENT_CURRENCY_MISMATCH, UNSUPPORTED_CURRENCY ->
                    ProblemDetailAssembler.unprocessable(messages, key);
            case PROJECT_NOT_FOUND, INVOICE_NOT_FOUND -> ProblemDetailAssembler.notFound(messages, key);
        };
    }
}
