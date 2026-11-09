package com.brasper.platform.staff.infrastructure.acl;

import com.brasper.platform.staff.application.outboundservices.StaffTreasuryLookup;
import com.brasper.platform.treasury.interfaces.acl.TreasuryContextFacade;
import org.springframework.stereotype.Component;

@Component
class StaffTreasuryFacadeLookup implements StaffTreasuryLookup {

    private final TreasuryContextFacade treasury;

    StaffTreasuryFacadeLookup(TreasuryContextFacade treasury) {
        this.treasury = treasury;
    }

    @Override
    public boolean bankAccountExists(Long bankAccountId) {
        return treasury.existsBankAccountById(bankAccountId);
    }
}
