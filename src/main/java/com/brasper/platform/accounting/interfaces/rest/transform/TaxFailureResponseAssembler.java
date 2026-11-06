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
            case DUPLICATE_EFFECTIVE_DATE -> ProblemDetailAssembler.conflict(messages, failure.messageKey());
        };
    }
}
