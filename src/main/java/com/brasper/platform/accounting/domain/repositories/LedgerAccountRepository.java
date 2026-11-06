package com.brasper.platform.accounting.domain.repositories;

import com.brasper.platform.accounting.domain.model.aggregates.LedgerAccount;
import com.brasper.platform.accounting.domain.model.valueobjects.AccountCode;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LedgerAccountRepository {
    LedgerAccount save(LedgerAccount account);
    Optional<LedgerAccount> findById(Long id);
    Optional<LedgerAccount> findByCode(AccountCode code);
    boolean existsByCode(AccountCode code);
    List<LedgerAccount> findAll();
    List<LedgerAccount> findAllByCodeIn(Collection<AccountCode> codes);
}
