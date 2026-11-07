package com.brasper.platform.accounting.interfaces.rest.transform;
import com.brasper.platform.accounting.application.commandservices.TaxCommandFailure;
import com.brasper.platform.shared.interfaces.rest.transform.ProblemDetailAssembler;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

public final class TaxFailureResponseAssembler {
    private TaxFailureResponseAssembler() { }
    public static ResponseEntity<ProblemDetail> toResponse(TaxCommandFailure failure, MessageSource messages) {
        return switch (failure) {
            case DUPLICATE_EFFECTIVE_DATE, DUPLICATE_RETURN, ALREADY_PAID, ACCOUNTS_NOT_CONFIGURED -> ProblemDetailAssembler.conflict(messages, failure.messageKey());
            case TAX_RETURN_NOT_FOUND -> ProblemDetailAssembler.notFound(messages, failure.messageKey());
            case BANK_ACCOUNT_NOT_FOUND -> ProblemDetailAssembler.badRequest(messages, failure.messageKey());
        };
    }
}
