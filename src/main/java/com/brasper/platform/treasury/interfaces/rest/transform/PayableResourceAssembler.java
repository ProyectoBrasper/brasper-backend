package com.brasper.platform.treasury.interfaces.rest.transform;

import com.brasper.platform.treasury.domain.model.aggregates.Payable;
import com.brasper.platform.treasury.interfaces.rest.resources.PayableResource;

public final class PayableResourceAssembler {

    private PayableResourceAssembler() {
    }

    public static PayableResource toResource(Payable payable) {
        var week = payable.getScheduledWeek();
        return new PayableResource(payable.getId(), payable.getConcept(), payable.getType().name(),
                payable.getTotalAmount().amount(), payable.getTotalAmount().currency().name(),
                payable.paidAmount().amount(), payable.balance().amount(), payable.getSupplierId(),
                payable.getProjectId(), payable.getExpenseId(), week.year(), week.month(), week.number(),
                payable.getStatus().name());
    }
}
