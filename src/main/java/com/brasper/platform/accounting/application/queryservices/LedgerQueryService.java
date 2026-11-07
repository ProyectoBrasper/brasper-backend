package com.brasper.platform.accounting.application.queryservices;

import com.brasper.platform.accounting.domain.model.aggregates.JournalEntry;
import com.brasper.platform.accounting.domain.model.aggregates.LedgerAccount;
import com.brasper.platform.accounting.domain.model.entities.JournalLine;
import com.brasper.platform.accounting.domain.model.valueobjects.AccountCode;
import com.brasper.platform.accounting.domain.model.valueobjects.TrialBalance;
import com.brasper.platform.accounting.domain.model.valueobjects.TrialBalanceLine;
import com.brasper.platform.accounting.domain.repositories.JournalEntryRepository;
import com.brasper.platform.accounting.domain.repositories.LedgerAccountRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
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
