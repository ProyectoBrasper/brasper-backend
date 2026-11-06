package com.brasper.platform.treasury.domain.repositories;

import com.brasper.platform.treasury.domain.model.aggregates.BankAccount;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountNumber;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountType;
import java.util.List;
import java.util.Optional;

public interface BankAccountRepository {

    BankAccount save(BankAccount bankAccount);

    Optional<BankAccount> findById(Long id);

    Optional<BankAccount> findByAccountNumber(AccountNumber accountNumber);

    List<BankAccount> findAllByType(AccountType type);

    boolean existsByAccountNumber(AccountNumber accountNumber);

    List<BankAccount> findAll();
}
