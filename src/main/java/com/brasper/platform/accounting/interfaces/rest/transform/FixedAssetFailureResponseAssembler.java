package com.brasper.platform.accounting.interfaces.rest.transform;
import com.brasper.platform.accounting.application.commandservices.FixedAssetCommandFailure;
import com.brasper.platform.shared.interfaces.rest.transform.ProblemDetailAssembler;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

public final class FixedAssetFailureResponseAssembler {
    private FixedAssetFailureResponseAssembler() { }
    public static ResponseEntity<ProblemDetail> toResponse(FixedAssetCommandFailure failure, MessageSource messages) {
        return switch (failure) {
            case FIXED_ASSET_NOT_FOUND -> ProblemDetailAssembler.notFound(messages, failure.messageKey());
            case INVALID_USEFUL_LIFE, INVALID_DISPOSAL_DATE -> ProblemDetailAssembler.badRequest(messages, failure.messageKey());
            case ALREADY_DISPOSED, NOTHING_TO_DEPRECIATE, DEPRECIATION_ALREADY_RUN, ACCOUNTS_NOT_CONFIGURED -> ProblemDetailAssembler.conflict(messages, failure.messageKey());
        };
    }
}
