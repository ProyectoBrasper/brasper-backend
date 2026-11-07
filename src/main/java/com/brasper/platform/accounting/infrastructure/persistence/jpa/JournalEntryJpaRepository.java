package com.brasper.platform.accounting.infrastructure.persistence.jpa;

import com.brasper.platform.accounting.domain.model.aggregates.JournalEntry;
import com.brasper.platform.accounting.domain.repositories.JournalEntryRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryJpaRepository extends JpaRepository<JournalEntry, Long>, JournalEntryRepository {
    List<JournalEntry> findAllByPeriodYearAndPeriodMonthOrderByEntryDateAscIdAsc(int year, int month);
    List<JournalEntry> findAllByEntryDateLessThanEqual(LocalDate date);
    @Override default List<JournalEntry> findAllUpTo(LocalDate date) { return findAllByEntryDateLessThanEqual(date); }
    @Override default List<JournalEntry> findAllByPeriod(Period period) {
        return findAllByPeriodYearAndPeriodMonthOrderByEntryDateAscIdAsc(period.year(), period.month());
    }
}
