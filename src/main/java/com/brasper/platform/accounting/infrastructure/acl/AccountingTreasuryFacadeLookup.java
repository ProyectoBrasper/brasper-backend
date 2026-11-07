package com.brasper.platform.accounting.infrastructure.acl;

import com.brasper.platform.accounting.application.outboundservices.AccountingTreasuryLookup;
import com.brasper.platform.treasury.interfaces.acl.TreasuryContextFacade;
import org.springframework.stereotype.Component;

@Component
class AccountingTreasuryFacadeLookup implements AccountingTreasuryLookup {

    private final TreasuryContextFacade treasury;

    AccountingTreasuryFacadeLookup(TreasuryContextFacade treasury) {
        this.treasury = treasury;
    }

    @Override
    public boolean bankAccountExists(Long bankAccountId) {
        return treasury.existsBankAccountById(bankAccountId);
    }
}
