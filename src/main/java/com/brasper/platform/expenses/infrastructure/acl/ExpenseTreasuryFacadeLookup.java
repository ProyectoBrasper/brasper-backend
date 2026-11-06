package com.brasper.platform.expenses.infrastructure.acl;

import com.brasper.platform.expenses.application.outboundservices.ExpenseTreasuryLookup;
import com.brasper.platform.treasury.interfaces.acl.TreasuryContextFacade;
import org.springframework.stereotype.Component;

@Component
class ExpenseTreasuryFacadeLookup implements ExpenseTreasuryLookup {

    private final TreasuryContextFacade treasury;

    ExpenseTreasuryFacadeLookup(TreasuryContextFacade treasury) {
        this.treasury = treasury;
    }

    @Override
    public boolean bankAccountExists(Long bankAccountId) {
        return treasury.existsBankAccountById(bankAccountId);
    }

    @Override
    public boolean partnerExists(Long partnerId) {
        return treasury.existsPartnerById(partnerId);
    }
}
