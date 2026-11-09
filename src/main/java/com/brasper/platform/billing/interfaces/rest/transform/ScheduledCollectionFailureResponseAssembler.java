package com.brasper.platform.billing.interfaces.rest.transform;

import com.brasper.platform.billing.application.commandservices.ScheduledCollectionCommandFailure;
import com.brasper.platform.shared.interfaces.rest.transform.ProblemDetailAssembler;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

public final class ScheduledCollectionFailureResponseAssembler {

    private ScheduledCollectionFailureResponseAssembler() {
    }

    public static ResponseEntity<ProblemDetail> toResponse(ScheduledCollectionCommandFailure failure,
            MessageSource messages) {
        String key = failure.messageKey();
        return switch (failure) {
            case PROJECT_NOT_FOUND, NOT_FOUND -> ProblemDetailAssembler.notFound(messages, key);
            case ALREADY_CLOSED -> ProblemDetailAssembler.conflict(messages, key);
            case UNSUPPORTED_CURRENCY -> ProblemDetailAssembler.unprocessable(messages, key);
        };
    }
}
