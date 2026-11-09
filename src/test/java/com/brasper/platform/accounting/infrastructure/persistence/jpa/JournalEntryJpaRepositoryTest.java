package com.brasper.platform.accounting.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.*;
import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.accounting.domain.model.aggregates.JournalEntry;
import com.brasper.platform.accounting.domain.model.commands.*;
import com.brasper.platform.accounting.domain.model.valueobjects.*;
import com.brasper.platform.accounting.domain.repositories.JournalEntryRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class JournalEntryJpaRepositoryTest {
    @Autowired private JournalEntryRepository entries;
    @Autowired private EntityManager em;
    private JournalEntry entry(LocalDate date, EntrySource source, Long sourceId) {
        Money amount = new Money(new BigDecimal("123.45"), Currency.USD), zero = Money.zero(Currency.USD);
        return new JournalEntry(new RegisterJournalEntryCommand(date, "Entry", source, sourceId, List.of(
                new JournalLineData(new AccountCode("10"), amount, zero, 42L),
                new JournalLineData(new AccountCode("70"), zero, amount, null))));
    }
    @Test void roundTripsLinesReversalAndOrderedPeriodQueries() {
        LocalDate date = LocalDate.of(2026, 11, 15);
        JournalEntry late = entries.save(entry(date.plusDays(1), EntrySource.INVOICE_ISSUED, 9L));
        JournalEntry first = entries.save(entry(date, EntrySource.MANUAL, null));
        JournalEntry second = entries.save(entry(date, EntrySource.MANUAL, null));
        entries.save(entry(date.minusMonths(1), EntrySource.MANUAL, null)); em.flush();
        Long lineId = late.getLines().getFirst().getId(); em.clear();
        JournalEntry found = entries.findById(late.getId()).orElseThrow();
        assertThat(found.getLines()).hasSize(2);
        assertThat(lineId).isNotNull();
        assertThat(found.getLines().getFirst()).extracting("id", "accountCode", "debit", "credit", "projectId", "journalEntry")
                .containsExactly(lineId, new AccountCode("10"), late.totalDebit(), Money.zero(Currency.USD), 42L, found);
        assertThat(found.getLines().getLast().getProjectId()).isNull();
        assertThat(found.totalCredit()).isEqualTo(late.totalCredit());
        assertThat(found.getCreatedAt()).isNotNull().isEqualTo(found.getUpdatedAt());
        assertThat(entries.existsBySourceAndSourceId(EntrySource.INVOICE_ISSUED, 9L)).isTrue();
        assertThat(entries.existsBySourceAndSourceId(EntrySource.EXPENSE, 9L)).isFalse();
        assertThat(entries.findAllByPeriod(new Period(2026, 11))).extracting(JournalEntry::getId)
                .containsExactly(first.getId(), second.getId(), late.getId());
        assertThat(entries.findAllByPeriod(new Period(2026, 12))).isEmpty();
        JournalEntry reversal = entries.save(found.reverse(date.plusMonths(1))); em.flush(); em.clear();
        assertThat(entries.findById(late.getId()).orElseThrow().isReversed()).isTrue();
        JournalEntry reloaded = entries.findById(reversal.getId()).orElseThrow();
        assertThat(reloaded).extracting("source", "sourceId", "reversalOfId", "period")
                .containsExactly(EntrySource.REVERSAL, late.getId(), late.getId(), new Period(2026, 12));
        assertThat(reloaded.getLines().getFirst().getCredit()).isEqualTo(found.totalDebit());
    }
    @Test void sumsEveryLineInTheDatabaseByAccountAndCurrencyForAPeriodARangeAndUpToADate() {
        AccountCode code = new AccountCode("9946");
        Money pen = new Money(new BigDecimal("100.10"), Currency.PEN), usd = new Money(new BigDecimal("7"), Currency.USD);
        List<Object[]> lines = List.of(
                new Object[] {LocalDate.of(2046, 4, 30), pen}, new Object[] {LocalDate.of(2046, 5, 3), pen},
                new Object[] {LocalDate.of(2046, 5, 20), pen}, new Object[] {LocalDate.of(2046, 5, 21), usd},
                new Object[] {LocalDate.of(2046, 6, 1), pen});
        for (Object[] line : lines) {
            Money amount = (Money) line[1], zero = Money.zero(amount.currency());
            entries.save(new JournalEntry(new RegisterJournalEntryCommand((LocalDate) line[0], "Sum", EntrySource.MANUAL, null,
                    List.of(new JournalLineData(code, amount, zero, null), new JournalLineData(new AccountCode("9947"), zero, amount, null)))));
        }
        em.flush();
        assertThat(totalOf(entries.totalsByPeriod(new Period(2046, 5)), code, Currency.PEN).debit()).isEqualByComparingTo("200.20");
        assertThat(totalOf(entries.totalsByPeriod(new Period(2046, 5)), code, Currency.USD).debit()).isEqualByComparingTo("7");
        assertThat(totalOf(entries.totalsBetween(LocalDate.of(2046, 5, 1), LocalDate.of(2046, 5, 20)), code, Currency.PEN).debit())
                .isEqualByComparingTo("200.20");
        assertThat(totalOf(entries.totalsUpTo(LocalDate.of(2046, 5, 31)), code, Currency.PEN).debit()).isEqualByComparingTo("300.30");
        assertThat(totalOf(entries.totalsUpTo(LocalDate.of(2046, 5, 31)), new AccountCode("9947"), Currency.PEN).credit())
                .isEqualByComparingTo("300.30");
    }
    private static AccountTotals totalOf(List<AccountTotals> totals, AccountCode code, Currency currency) {
        return totals.stream().filter(t -> t.accountCode().equals(code) && t.currency() == currency).findFirst().orElseThrow();
    }
    @Test void rejectsDuplicateSourceButAllowsOtherSourcesAndManualEntries() {
        LocalDate date = LocalDate.of(2026, 11, 1);
        entries.save(entry(date, EntrySource.MANUAL, null));
        entries.save(entry(date, EntrySource.MANUAL, null));
        entries.save(entry(date, EntrySource.EXPENSE, 9L));
        entries.save(entry(date, EntrySource.INVOICE_ISSUED, 9L)); em.flush();
        assertThatThrownBy(() -> { entries.save(entry(date, EntrySource.INVOICE_ISSUED, 9L)); em.flush(); })
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
