package com.brasper.platform.accounting.infrastructure.persistence.jpa;

import com.brasper.platform.accounting.domain.model.aggregates.LedgerAccount;
import com.brasper.platform.accounting.domain.repositories.LedgerAccountRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerAccountJpaRepository extends JpaRepository<LedgerAccount, Long>, LedgerAccountRepository { }
