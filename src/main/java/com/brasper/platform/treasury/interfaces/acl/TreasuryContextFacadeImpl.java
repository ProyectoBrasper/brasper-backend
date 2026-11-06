package com.brasper.platform.treasury.interfaces.acl;

import com.brasper.platform.treasury.application.queryservices.BankAccountQueryService;
import com.brasper.platform.treasury.application.queryservices.PartnerQueryService;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Lives in {@code interfaces} (the UML puts it in {@code application}) because the layer rules forbid
 * application from depending on interfaces, where the facade contract lives.
 * shortcut: cash balances and weekly sums from the UML are left out until Reporting needs them.
 */
@Service
public class TreasuryContextFacadeImpl implements TreasuryContextFacade {

    private final BankAccountQueryService accounts;
    private final PartnerQueryService partners;

    public TreasuryContextFacadeImpl(BankAccountQueryService accounts, PartnerQueryService partners) {
        this.accounts = accounts;
        this.partners = partners;
    }

    @Override
    public boolean existsBankAccountById(Long bankAccountId) {
        return accounts.findById(bankAccountId).isPresent();
    }

    @Override
    public Optional<String> fetchBankAccountType(Long bankAccountId) {
        return accounts.findById(bankAccountId).map(summary -> summary.account().getType().name());
    }

    @Override
    public boolean existsPartnerById(Long partnerId) {
        return partners.findById(partnerId).isPresent();
    }
}
