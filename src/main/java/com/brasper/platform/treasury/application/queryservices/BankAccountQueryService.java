package com.brasper.platform.treasury.application.queryservices;

import com.brasper.platform.treasury.domain.model.aggregates.BankAccount;
import com.brasper.platform.treasury.domain.model.entities.CashMovement;
import com.brasper.platform.treasury.domain.model.queries.GetCashMovementsByAccountAndPeriodQuery;
import com.brasper.platform.treasury.domain.repositories.BankAccountRepository;
import com.brasper.platform.treasury.domain.repositories.CashMovementRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BankAccountQueryService {

    private final BankAccountRepository accounts;
    private final CashMovementRepository movements;

    public BankAccountQueryService(BankAccountRepository accounts, CashMovementRepository movements) {
        this.accounts = accounts;
        this.movements = movements;
    }

    public List<BankAccountSummary> findAll() {
        return accounts.findAll().stream().map(BankAccountQueryService::summarize).toList();
    }

    public Optional<BankAccountSummary> findById(Long id) {
        return accounts.findById(id).map(BankAccountQueryService::summarize);
    }

    /** Empty when the account does not exist. */
    public Optional<List<CashMovement>> handle(GetCashMovementsByAccountAndPeriodQuery query) {
        return accounts.findById(query.bankAccountId()).map(account -> movements
                .findAllByBankAccountIdAndOccurredOnBetween(account.getId(), query.period().startDate(),
                        query.period().endDate()));
    }

    private static BankAccountSummary summarize(BankAccount account) {
        return new BankAccountSummary(account, account.currentBalance());
    }
}
