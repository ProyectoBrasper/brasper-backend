package com.brasper.platform.iam.interfaces.rest.transform;

import com.brasper.platform.iam.application.commandservices.UserCommandFailure;
import com.brasper.platform.shared.interfaces.rest.transform.ProblemDetailAssembler;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

public final class UserFailureResponseAssembler {

    private UserFailureResponseAssembler() {
    }

    public static ResponseEntity<ProblemDetail> toResponse(UserCommandFailure failure, MessageSource messages) {
        String key = failure.messageKey();
        return switch (failure) {
            case INVALID_CREDENTIALS -> ProblemDetailAssembler.unauthorized(messages, key);
            case USER_INACTIVE -> ProblemDetailAssembler.forbidden(messages, key);
            case DUPLICATE_USERNAME -> ProblemDetailAssembler.conflict(messages, key);
            case WEAK_PASSWORD -> ProblemDetailAssembler.unprocessable(messages, key);
            case USER_NOT_FOUND -> ProblemDetailAssembler.notFound(messages, key);
            case CANNOT_MODIFY_LAST_ADMIN -> ProblemDetailAssembler.conflict(messages, key);
        };
    }
}
