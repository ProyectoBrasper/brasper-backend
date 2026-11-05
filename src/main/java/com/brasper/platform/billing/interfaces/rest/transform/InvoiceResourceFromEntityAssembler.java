package com.brasper.platform.billing.interfaces.rest.transform;

import com.brasper.platform.billing.domain.model.aggregates.Invoice;
import com.brasper.platform.billing.interfaces.rest.resources.InvoiceResource;

public final class InvoiceResourceFromEntityAssembler {

    private InvoiceResourceFromEntityAssembler() {
    }

    public static InvoiceResource toResourceFromEntity(Invoice invoice) {
        var amounts = invoice.getAmounts();
        return new InvoiceResource(invoice.getId(), invoice.getNumber().toString(), invoice.getIssuedOn(),
                invoice.getPurchaseOrder(), invoice.getProjectId(), invoice.getClientId(),
                amounts.total().currency().name(), amounts.taxableBase().amount(), amounts.igv().amount(),
                amounts.total().amount(), invoice.getRetention().amount(), invoice.getDetraction().amount(),
                invoice.isSelfDetraction(), invoice.isAdvance(), invoice.netAmount().amount(),
                invoice.collectedAmount().amount(), invoice.outstandingBalance().amount(),
                invoice.getStatus().name(), invoice.getNotes());
    }
}
