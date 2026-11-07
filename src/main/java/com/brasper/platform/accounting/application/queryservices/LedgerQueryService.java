package com.brasper.platform.accounting.application.queryservices;

import com.brasper.platform.accounting.domain.model.aggregates.JournalEntry;
import com.brasper.platform.accounting.domain.model.aggregates.LedgerAccount;
import com.brasper.platform.accounting.domain.model.entities.JournalLine;
import com.brasper.platform.accounting.domain.model.valueobjects.AccountCode;
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
import java.util.function.Function;
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
        return balances(entries.findAllUpTo(date));
    }

    public Map<String, BigDecimal> balancesByTypeAt(LocalDate date) {
        return byType(accountBalancesAt(date));
    }

    /** REVENUE, EXPENSE and RESULT (revenue minus expense) of the period. */
    public Map<String, BigDecimal> resultByPeriod(Period period) {
        Map<String, BigDecimal> types = byType(balances(entries.findAllByPeriod(period)));
        BigDecimal revenue = types.getOrDefault(LedgerAccountType.REVENUE.name(), BigDecimal.ZERO);
        BigDecimal expense = types.getOrDefault(LedgerAccountType.EXPENSE.name(), BigDecimal.ZERO);
        return Map.of("REVENUE", revenue, "EXPENSE", expense, "RESULT", revenue.subtract(expense));
    }

    // shortcut: PEN lines only and summed in memory, like the trial balance; group in the database when volumes ask for it.
    private Map<String, BigDecimal> balances(List<JournalEntry> list) {
        Map<String, LedgerAccountType> types = accounts.findAll().stream()
                .collect(Collectors.toMap(a -> a.getCode().value(), LedgerAccount::getType));
        Map<String, BigDecimal> balances = new TreeMap<>();
        for (JournalEntry entry : list) {
            for (JournalLine line : entry.getLines()) {
                if (line.getDebit().currency() != Currency.PEN) {
                    continue;
                }
                String code = line.getAccountCode().value();
                BigDecimal net = line.getDebit().amount().subtract(line.getCredit().amount());
                boolean debitNormal = types.get(code) == LedgerAccountType.ASSET || types.get(code) == LedgerAccountType.EXPENSE;
                balances.merge(code, debitNormal ? net : net.negate(), BigDecimal::add);
            }
        }
        return balances;
    }

    private Map<String, BigDecimal> byType(Map<String, BigDecimal> balances) {
        Map<String, LedgerAccountType> types = accounts.findAll().stream()
                .collect(Collectors.toMap(a -> a.getCode().value(), LedgerAccount::getType));
        Map<String, BigDecimal> totals = new TreeMap<>();
        balances.forEach((code, balance) -> totals.merge(types.get(code).name(), balance, BigDecimal::add));
        return totals;
    }

    /**
     * Movement of the period per account, from the entries dated in it. One line per account and currency.
     * shortcut: the period's movement only, no opening balances or running totals, and summed in memory;
     * move to a grouped query if a month ever holds more entries than fit comfortably in memory.
     */
    public TrialBalance trialBalance(Period period) {
        Map<String, String> names = accounts.findAll().stream()
                .collect(Collectors.toMap(a -> a.getCode().value(), LedgerAccount::getName));
        Map<String, TrialBalanceLine> totals = new TreeMap<>();
        for (JournalEntry entry : entries.findAllByPeriod(period)) {
            for (JournalLine line : entry.getLines()) {
                AccountCode code = line.getAccountCode();
                Currency currency = line.getDebit().currency();
                totals.merge(code.value() + "|" + currency,
                        new TrialBalanceLine(code, names.getOrDefault(code.value(), code.value()),
                                line.getDebit(), line.getCredit()),
                        (a, b) -> new TrialBalanceLine(a.accountCode(), a.accountName(),
                                a.debit().add(b.debit()), a.credit().add(b.credit())));
            }
        }
        return new TrialBalance(period, List.copyOf(totals.values()));
    }
}
