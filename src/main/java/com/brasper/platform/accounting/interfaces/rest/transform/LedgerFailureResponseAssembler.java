package com.brasper.platform.accounting.interfaces.rest.transform;
import com.brasper.platform.accounting.application.commandservices.LedgerCommandFailure;
import com.brasper.platform.shared.interfaces.rest.transform.ProblemDetailAssembler;
import org.springframework.context.MessageSource;
import org.springframework.http.*;

public final class LedgerFailureResponseAssembler {
    private LedgerFailureResponseAssembler() { }
    public static ResponseEntity<ProblemDetail> toResponse(LedgerCommandFailure failure, MessageSource messages) {
        return switch (failure) {
            case DUPLICATE_ACCOUNT_CODE, DUPLICATE_SOURCE, ALREADY_REVERSED -> ProblemDetailAssembler.conflict(messages, failure.messageKey());
            case ACCOUNT_NOT_FOUND, ENTRY_NOT_FOUND -> ProblemDetailAssembler.notFound(messages, failure.messageKey());
            case INVALID_ENTRY, ACCOUNT_INACTIVE -> ProblemDetailAssembler.badRequest(messages, failure.messageKey());
        };
    }
}
