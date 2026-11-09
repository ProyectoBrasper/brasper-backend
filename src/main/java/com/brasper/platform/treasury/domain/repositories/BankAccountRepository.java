package com.brasper.platform.treasury.domain.repositories;

import java.time.LocalDate;
import java.math.BigDecimal;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementTotal;
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
    /** Opening balances of the accounts open on the date plus their movements up to it; currencies are added as plain numbers. */
    BigDecimal sumBalancesAt(LocalDate date);
    /** Movements between the dates, both included, by direction and source. Transfers between own accounts are left out. */
    List<MovementTotal> sumMovementsBetween(LocalDate from, LocalDate to);
}
