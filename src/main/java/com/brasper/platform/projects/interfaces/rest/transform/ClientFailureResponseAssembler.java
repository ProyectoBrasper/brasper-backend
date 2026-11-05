package com.brasper.platform.projects.interfaces.rest.transform;

import com.brasper.platform.projects.application.commandservices.ClientCommandFailure;
import com.brasper.platform.shared.interfaces.rest.transform.ProblemDetailAssembler;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

public final class ClientFailureResponseAssembler {

    private ClientFailureResponseAssembler() {
    }

    public static ResponseEntity<ProblemDetail> toResponse(ClientCommandFailure failure, MessageSource messages) {
        String key = failure.messageKey();
        return switch (failure) {
            case DUPLICATE_RUC -> ProblemDetailAssembler.conflict(messages, key);
            case CLIENT_NOT_FOUND -> ProblemDetailAssembler.notFound(messages, key);
        };
    }
}
