package com.brasper.platform.expenses.interfaces.rest.transform;

import com.brasper.platform.expenses.application.commandservices.SupplierCommandFailure;
import com.brasper.platform.shared.interfaces.rest.transform.ProblemDetailAssembler;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

public final class SupplierFailureResponseAssembler {

    private SupplierFailureResponseAssembler() {
    }

    public static ResponseEntity<ProblemDetail> toResponse(SupplierCommandFailure failure, MessageSource messages) {
        String key = failure.messageKey();
        return switch (failure) {
            case DUPLICATE_RUC -> ProblemDetailAssembler.conflict(messages, key);
            case RUC_REQUIRED_FOR_COMPANY -> ProblemDetailAssembler.badRequest(messages, key);
            case SUPPLIER_NOT_FOUND -> ProblemDetailAssembler.notFound(messages, key);
        };
    }
}
