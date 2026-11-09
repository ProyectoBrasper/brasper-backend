package com.brasper.platform.treasury.interfaces.rest.transform;

import com.brasper.platform.shared.interfaces.rest.transform.ProblemDetailAssembler;
import com.brasper.platform.treasury.application.commandservices.PartnerCommandFailure;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

public final class PartnerFailureResponseAssembler {

    private PartnerFailureResponseAssembler() {
    }

    public static ResponseEntity<ProblemDetail> toResponse(PartnerCommandFailure failure, MessageSource messages) {
        String key = failure.messageKey();
        return switch (failure) {
            case PARTNER_NOT_FOUND, CONTRIBUTION_ACCOUNT_NOT_FOUND -> ProblemDetailAssembler.notFound(messages, key);
            case DUPLICATE_PARTNER -> ProblemDetailAssembler.conflict(messages, key);
            case INVALID_AMOUNT -> ProblemDetailAssembler.badRequest(messages, key);
            case UNSUPPORTED_CURRENCY -> ProblemDetailAssembler.unprocessable(messages, key);
        };
    }
}
