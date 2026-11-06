package com.brasper.platform.treasury.interfaces.acl;

import java.util.Optional;

/** The only way other contexts ask Treasury about bank accounts and partners. They keep the id, nothing else. */
public interface TreasuryContextFacade {

    boolean existsBankAccountById(Long bankAccountId);

    /** The {@code AccountType} name, e.g. {@code CHECKING}; empty when the account does not exist. */
    Optional<String> fetchBankAccountType(Long bankAccountId);

    boolean existsPartnerById(Long partnerId);
}
