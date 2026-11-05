package com.brasper.platform.billing.interfaces.rest.transform;

import com.brasper.platform.billing.domain.model.entities.InvoicePayment;
import com.brasper.platform.billing.interfaces.rest.resources.InvoicePaymentResource;

public final class InvoicePaymentResourceFromEntityAssembler {

    private InvoicePaymentResourceFromEntityAssembler() {
    }

    public static InvoicePaymentResource toResourceFromEntity(InvoicePayment payment) {
        return new InvoicePaymentResource(payment.getId(), payment.getReceivedOn(), payment.getAmount().amount(),
                payment.getType().name(), payment.getBankAccountId(), payment.getReference());
    }
}
