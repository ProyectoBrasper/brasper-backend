package com.brasper.platform.accounting.domain.repositories;

import com.brasper.platform.accounting.domain.model.aggregates.JournalEntry;
import com.brasper.platform.accounting.domain.model.valueobjects.EntrySource;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import com.brasper.platform.accounting.domain.model.valueobjects.AccountCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JournalEntryRepository {
    JournalEntry save(JournalEntry entity);
    Optional<JournalEntry> findById(Long id);
    Optional<JournalEntry> findBySourceAndSourceId(EntrySource source, Long sourceId);
    /** Credits minus debits of the account on the lines tagged with the project; PEN, currencies are not separated. */
    BigDecimal sumCreditBalanceByAccountAndProject(AccountCode account, Long projectId);
    boolean existsBySourceAndSourceId(EntrySource source, Long sourceId);
    List<JournalEntry> findAllByPeriod(Period period);
    List<JournalEntry> findAllUpTo(LocalDate date);
    List<JournalEntry> findAllBetween(LocalDate from, LocalDate to);
}
