package com.brasper.platform.staff.interfaces.rest.transform;

import com.brasper.platform.staff.domain.model.aggregates.FixedCost;
import com.brasper.platform.staff.domain.model.entities.FixedCostPayment;
import com.brasper.platform.staff.interfaces.rest.resources.*;

public final class FixedCostResourceAssembler {
    private FixedCostResourceAssembler() {}
    public static FixedCostResource toResource(FixedCost cost) {
        return new FixedCostResource(cost.getId(), cost.getCode().value(), cost.getConcept(), cost.getType(),
                cost.getMonthlyAmount().amount(), cost.getCurrency(), cost.getEmployeeId(), cost.getExpectedWeeks(), cost.isActive());
    }
    public static FixedCostPaymentResource toResource(Long costId, FixedCostPayment payment) {
        return new FixedCostPaymentResource(payment.getId(), costId, payment.getPaidOn(), payment.getPeriod().year(),
                payment.getPeriod().month(), payment.getAmount().amount(), payment.getAmount().currency(),
                payment.getBankAccountId(), payment.getNotes());
    }
}
