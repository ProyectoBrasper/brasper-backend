package com.brasper.platform.accounting.application.commandservices;

import com.brasper.platform.accounting.domain.model.aggregates.JournalEntry;
import com.brasper.platform.accounting.domain.model.aggregates.LedgerAccount;
import com.brasper.platform.accounting.domain.model.commands.CreateLedgerAccountCommand;
import com.brasper.platform.accounting.domain.model.commands.JournalLineData;
import com.brasper.platform.accounting.domain.model.commands.RegisterJournalEntryCommand;
import com.brasper.platform.accounting.domain.model.commands.ReverseJournalEntryCommand;
import com.brasper.platform.accounting.domain.model.valueobjects.AccountCode;
import com.brasper.platform.accounting.domain.model.valueobjects.EntrySource;
import com.brasper.platform.accounting.domain.repositories.JournalEntryRepository;
import com.brasper.platform.accounting.domain.repositories.LedgerAccountRepository;
import com.brasper.platform.shared.application.result.Result;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerCommandService {

    private final LedgerAccountRepository accounts;
    private final JournalEntryRepository entries;

    public LedgerCommandService(LedgerAccountRepository accounts, JournalEntryRepository entries) {
        this.accounts = accounts;
        this.entries = entries;
    }

    @Transactional
    public Result<LedgerAccount, LedgerCommandFailure> handle(CreateLedgerAccountCommand command) {
        if (accounts.existsByCode(command.code())) {
            return Result.failure(LedgerCommandFailure.DUPLICATE_ACCOUNT_CODE);
        }
        return Result.success(accounts.save(new LedgerAccount(command)));
    }

    /** The aggregate enforces the double-entry rules; any violation is reported as an invalid entry. */
    @Transactional
    public Result<JournalEntry, LedgerCommandFailure> handle(RegisterJournalEntryCommand command) {
        JournalEntry entry;
        try {
            entry = new JournalEntry(command);
        } catch (IllegalArgumentException invalid) {
            return Result.failure(LedgerCommandFailure.INVALID_ENTRY);
        }
        if (command.sourceId() != null && entries.existsBySourceAndSourceId(command.source(), command.sourceId())) {
            return Result.failure(LedgerCommandFailure.DUPLICATE_SOURCE);
        }
        return accountFailure(command.lines())
                .<Result<JournalEntry, LedgerCommandFailure>>map(Result::failure)
                .orElseGet(() -> Result.success(entries.save(entry)));
    }

    @Transactional
    public Result<JournalEntry, LedgerCommandFailure> handle(ReverseJournalEntryCommand command) {
        JournalEntry entry = entries.findById(command.journalEntryId()).orElse(null);
        if (entry == null) {
            return Result.failure(LedgerCommandFailure.ENTRY_NOT_FOUND);
        }
        if (entry.isReversed() || entry.getSource() == EntrySource.REVERSAL) {
            return Result.failure(LedgerCommandFailure.ALREADY_REVERSED);
        }
        JournalEntry reversal = entry.reverse(command.reversedOn());
        entries.save(entry);
        return Result.success(entries.save(reversal));
    }

    private Optional<LedgerCommandFailure> accountFailure(List<JournalLineData> lines) {
        Set<AccountCode> codes = lines.stream().map(JournalLineData::accountCode).collect(Collectors.toSet());
        Map<AccountCode, LedgerAccount> found = accounts.findAllByCodeIn(codes).stream()
                .collect(Collectors.toMap(LedgerAccount::getCode, Function.identity()));
        if (!found.keySet().containsAll(codes)) {
            return Optional.of(LedgerCommandFailure.ACCOUNT_NOT_FOUND);
        }
        return found.values().stream().allMatch(LedgerAccount::isActive)
                ? Optional.empty() : Optional.of(LedgerCommandFailure.ACCOUNT_INACTIVE);
    }
}
