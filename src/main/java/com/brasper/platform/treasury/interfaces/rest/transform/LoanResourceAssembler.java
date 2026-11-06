package com.brasper.platform.treasury.interfaces.rest.transform;

import com.brasper.platform.treasury.domain.model.aggregates.Loan;
import com.brasper.platform.treasury.domain.model.entities.LoanInstallment;
import com.brasper.platform.treasury.interfaces.rest.resources.LoanResource;
import com.brasper.platform.treasury.interfaces.rest.resources.LoanResource.InstallmentResource;

public final class LoanResourceAssembler {

    private LoanResourceAssembler() {
    }

    public static LoanResource toResource(Loan loan) {
        return new LoanResource(loan.getId(), loan.getType().name(), loan.getCounterparty(),
                loan.getPrincipal().amount(), loan.getPrincipal().currency().name(), loan.getInterestRate().value(),
                loan.getDisbursedOn(), loan.getBankAccountId(), loan.totalInterest().amount(),
                loan.outstandingBalance().amount(), loan.getStatus().name(),
                loan.getInstallments().stream().map(LoanResourceAssembler::toResource).toList());
    }

    public static InstallmentResource toResource(LoanInstallment installment) {
        return new InstallmentResource(installment.getId(), installment.getDueOn(), installment.getPrincipal().amount(),
                installment.getInterest().amount(), installment.isPaid(), installment.getPaidOn());
    }
}
