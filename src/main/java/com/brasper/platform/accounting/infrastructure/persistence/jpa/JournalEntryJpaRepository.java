package com.brasper.platform.accounting.infrastructure.persistence.jpa;

import com.brasper.platform.accounting.domain.model.aggregates.JournalEntry;
import com.brasper.platform.accounting.domain.repositories.JournalEntryRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.time.LocalDate;
import java.util.List;
import com.brasper.platform.accounting.domain.model.valueobjects.AccountCode;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JournalEntryJpaRepository extends JpaRepository<JournalEntry, Long>, JournalEntryRepository {
    @Override
    @Query("select coalesce(sum(l.credit - l.debit), 0) from JournalLine l where l.accountCode = :account and l.projectId = :projectId")
    BigDecimal sumCreditBalanceByAccountAndProject(@Param("account") AccountCode account, @Param("projectId") Long projectId);

    List<JournalEntry> findAllByPeriodYearAndPeriodMonthOrderByEntryDateAscIdAsc(int year, int month);
    List<JournalEntry> findAllByEntryDateLessThanEqual(LocalDate date);
    List<JournalEntry> findAllByEntryDateBetween(LocalDate from, LocalDate to);
    @Override default List<JournalEntry> findAllBetween(LocalDate from, LocalDate to) { return findAllByEntryDateBetween(from, to); }
    @Override default List<JournalEntry> findAllUpTo(LocalDate date) { return findAllByEntryDateLessThanEqual(date); }
    @Override default List<JournalEntry> findAllByPeriod(Period period) {
        return findAllByPeriodYearAndPeriodMonthOrderByEntryDateAscIdAsc(period.year(), period.month());
    }
}
