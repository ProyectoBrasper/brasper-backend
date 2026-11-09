package com.brasper.platform.accounting.infrastructure.persistence.jpa;

import com.brasper.platform.accounting.domain.model.aggregates.JournalEntry;
import com.brasper.platform.accounting.domain.repositories.JournalEntryRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.time.LocalDate;
import java.util.List;
import com.brasper.platform.accounting.domain.model.valueobjects.AccountCode;
import com.brasper.platform.accounting.domain.model.valueobjects.AccountTotals;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JournalEntryJpaRepository extends JpaRepository<JournalEntry, Long>, JournalEntryRepository {
    @Override
    @Query("select coalesce(sum(l.credit - l.debit), 0) from JournalLine l where l.accountCode = :account and l.projectId = :projectId")
    BigDecimal sumCreditBalanceByAccountAndProject(@Param("account") AccountCode account, @Param("projectId") Long projectId);

    List<JournalEntry> findAllByPeriodYearAndPeriodMonthOrderByEntryDateAscIdAsc(int year, int month);

    String TOTALS = "select new com.brasper.platform.accounting.domain.model.valueobjects.AccountTotals("
            + "l.accountCode, l.currency, sum(l.debit), sum(l.credit)) from JournalLine l where ";
    String GROUP = " group by l.accountCode, l.currency";

    @Override
    @Query(TOTALS + "l.journalEntry.entryDate <= :date" + GROUP)
    List<AccountTotals> totalsUpTo(@Param("date") LocalDate date);

    @Override
    @Query(TOTALS + "l.journalEntry.entryDate between :from and :to" + GROUP)
    List<AccountTotals> totalsBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(TOTALS + "l.journalEntry.periodYear = :year and l.journalEntry.periodMonth = :month" + GROUP)
    List<AccountTotals> totalsByPeriod(@Param("year") int year, @Param("month") int month);

    @Override default List<AccountTotals> totalsByPeriod(Period period) {
        return totalsByPeriod(period.year(), period.month());
    }
    @Override default List<JournalEntry> findAllByPeriod(Period period) {
        return findAllByPeriodYearAndPeriodMonthOrderByEntryDateAscIdAsc(period.year(), period.month());
    }
}
