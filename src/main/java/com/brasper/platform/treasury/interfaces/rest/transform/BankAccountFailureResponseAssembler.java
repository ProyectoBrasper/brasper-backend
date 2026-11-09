package com.brasper.platform.treasury.interfaces.rest.transform;

import com.brasper.platform.shared.interfaces.rest.transform.ProblemDetailAssembler;
import com.brasper.platform.treasury.application.commandservices.BankAccountCommandFailure;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

public final class BankAccountFailureResponseAssembler {

    private BankAccountFailureResponseAssembler() {
    }

    public static ResponseEntity<ProblemDetail> toResponse(BankAccountCommandFailure failure, MessageSource messages) {
        String key = failure.messageKey();
        return switch (failure) {
            case DUPLICATE_ACCOUNT_NUMBER -> ProblemDetailAssembler.conflict(messages, key);
            case BANK_ACCOUNT_NOT_FOUND -> ProblemDetailAssembler.notFound(messages, key);
            case SAME_ACCOUNT, CURRENCY_MISMATCH -> ProblemDetailAssembler.badRequest(messages, key);
            case UNSUPPORTED_CURRENCY -> ProblemDetailAssembler.unprocessable(messages, key);
        };
    }
}
