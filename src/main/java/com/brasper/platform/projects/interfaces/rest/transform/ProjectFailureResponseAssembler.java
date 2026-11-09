package com.brasper.platform.projects.interfaces.rest.transform;

import com.brasper.platform.projects.application.commandservices.ProjectCommandFailure;
import com.brasper.platform.shared.interfaces.rest.transform.ProblemDetailAssembler;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

public final class ProjectFailureResponseAssembler {

    private ProjectFailureResponseAssembler() {
    }

    public static ResponseEntity<ProblemDetail> toResponse(ProjectCommandFailure failure, MessageSource messages) {
        String key = failure.messageKey();
        return switch (failure) {
            case DUPLICATE_CODE, INVALID_STATUS_TRANSITION, PERIOD_ALREADY_REGISTERED ->
                    ProblemDetailAssembler.conflict(messages, key);
            case PROGRESS_EXCEEDS_LIMIT, COST_CURRENCY_MISMATCH, UNSUPPORTED_CURRENCY ->
                    ProblemDetailAssembler.unprocessable(messages, key);
            case CLIENT_NOT_FOUND, PROJECT_NOT_FOUND -> ProblemDetailAssembler.notFound(messages, key);
        };
    }
}
