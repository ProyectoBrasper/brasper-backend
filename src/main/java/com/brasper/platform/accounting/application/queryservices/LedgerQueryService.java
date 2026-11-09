package com.brasper.platform.accounting.application.queryservices;

import com.brasper.platform.accounting.domain.model.aggregates.JournalEntry;
import com.brasper.platform.accounting.domain.model.aggregates.LedgerAccount;
import com.brasper.platform.accounting.domain.model.valueobjects.AccountTotals;
import com.brasper.platform.accounting.domain.model.valueobjects.LedgerAccountType;
import com.brasper.platform.accounting.domain.model.valueobjects.TrialBalance;
import com.brasper.platform.accounting.domain.model.valueobjects.TrialBalanceLine;
import com.brasper.platform.accounting.domain.repositories.JournalEntryRepository;
import com.brasper.platform.accounting.domain.repositories.LedgerAccountRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LedgerQueryService {

    private final LedgerAccountRepository accounts;
    private final JournalEntryRepository entries;

    public LedgerQueryService(LedgerAccountRepository accounts, JournalEntryRepository entries) {
        this.accounts = accounts;
        this.entries = entries;
    }

    public List<LedgerAccount> findAllAccounts() {
        return accounts.findAll().stream().sorted(Comparator.comparing(a -> a.getCode().value())).toList();
    }

    public List<JournalEntry> findEntriesByPeriod(Period period) {
        return entries.findAllByPeriod(period);
    }

    public Optional<JournalEntry> findEntryById(Long id) {
        return entries.findById(id);
    }

    /** Balance of each account code up to and including the date: debit minus credit for assets and expenses, the reverse otherwise. */
    public Map<String, BigDecimal> accountBalancesAt(LocalDate date) {
        return balances(entries.totalsUpTo(date), accountTypes());
    }

    /** Movement per account code over the date range, signed like the balances. */
    public Map<String, BigDecimal> accountMovementsBetween(LocalDate from, LocalDate to) {
        return balances(entries.totalsBetween(from, to), accountTypes());
    }

    public Map<String, BigDecimal> balancesByTypeAt(LocalDate date) {
        Map<String, LedgerAccountType> types = accountTypes();
        return byType(balances(entries.totalsUpTo(date), types), types);
    }

    /** REVENUE, EXPENSE and RESULT (revenue minus expense) of the period. */
    public Map<String, BigDecimal> resultByPeriod(Period period) {
        Map<String, LedgerAccountType> types = accountTypes();
        Map<String, BigDecimal> byType = byType(balances(entries.totalsByPeriod(period), types), types);
        BigDecimal revenue = byType.getOrDefault(LedgerAccountType.REVENUE.name(), BigDecimal.ZERO);
        BigDecimal expense = byType.getOrDefault(LedgerAccountType.EXPENSE.name(), BigDecimal.ZERO);
        return Map.of("REVENUE", revenue, "EXPENSE", expense, "RESULT", revenue.subtract(expense));
    }

    private Map<String, LedgerAccountType> accountTypes() {
        return accounts.findAll().stream().collect(Collectors.toMap(a -> a.getCode().value(), LedgerAccount::getType));
    }

    // shortcut: PEN totals only; foreign currencies need an exchange rate first.
    private static Map<String, BigDecimal> balances(List<AccountTotals> totals, Map<String, LedgerAccountType> types) {
        Map<String, BigDecimal> balances = new TreeMap<>();
        for (AccountTotals total : totals) {
            if (total.currency() != Currency.PEN) {
                continue;
            }
            String code = total.accountCode().value();
            BigDecimal net = total.debit().subtract(total.credit());
            boolean debitNormal = types.get(code) == LedgerAccountType.ASSET || types.get(code) == LedgerAccountType.EXPENSE;
            balances.merge(code, debitNormal ? net : net.negate(), BigDecimal::add);
        }
        return balances;
    }

    private static Map<String, BigDecimal> byType(Map<String, BigDecimal> balances, Map<String, LedgerAccountType> types) {
        Map<String, BigDecimal> totals = new TreeMap<>();
        balances.forEach((code, balance) -> totals.merge(types.get(code).name(), balance, BigDecimal::add));
        return totals;
    }

    /** Movement of the period per account, from the entries dated in it. One line per account and currency. */
    public TrialBalance trialBalance(Period period) {
        Map<String, String> names = accounts.findAll().stream()
                .collect(Collectors.toMap(a -> a.getCode().value(), LedgerAccount::getName));
        List<TrialBalanceLine> lines = entries.totalsByPeriod(period).stream()
                .sorted(Comparator.comparing((AccountTotals t) -> t.accountCode().value() + "|" + t.currency()))
                .map(t -> new TrialBalanceLine(t.accountCode(), names.getOrDefault(t.accountCode().value(), t.accountCode().value()),
                        new Money(t.debit(), t.currency()), new Money(t.credit(), t.currency())))
                .toList();
        return new TrialBalance(period, lines);
    }
}
