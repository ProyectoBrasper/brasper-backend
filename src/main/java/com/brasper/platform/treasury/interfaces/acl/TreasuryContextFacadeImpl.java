package com.brasper.platform.treasury.interfaces.acl;

import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.treasury.application.queryservices.BankAccountQueryService;
import com.brasper.platform.treasury.application.queryservices.CashFigures;
import com.brasper.platform.treasury.application.queryservices.PartnerQueryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Lives in {@code interfaces} (the UML puts it in {@code application}) because the layer rules forbid
 * application from depending on interfaces, where the facade contract lives.
 * shortcut: the cash figures are summed in memory (see CashFigures).
 */
@Service
public class TreasuryContextFacadeImpl implements TreasuryContextFacade {

    private final BankAccountQueryService accounts;
    private final PartnerQueryService partners;
    private final CashFigures cash;

    public TreasuryContextFacadeImpl(BankAccountQueryService accounts, PartnerQueryService partners, CashFigures cash) {
        this.accounts = accounts;
        this.partners = partners;
        this.cash = cash;
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

    @Override
    public BigDecimal fetchCashBalanceAt(LocalDate date) {
        return cash.balanceAt(date);
    }

    @Override
    public Map<String, BigDecimal> fetchMovementsBetween(LocalDate from, LocalDate to) {
        return cash.movementsBetween(from, to);
    }

    @Override
    public BigDecimal sumScheduledPayablesByWeek(int year, int month, int week) {
        return cash.scheduledPayables(new CashFlowWeek(year, month, week));
    }

    @Override
    public Map<String, BigDecimal> sumScheduledInstallmentsByWeek(int year, int month, int week) {
        return cash.scheduledInstallments(new CashFlowWeek(year, month, week));
    }
}
