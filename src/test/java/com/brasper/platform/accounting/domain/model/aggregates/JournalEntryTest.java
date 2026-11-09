package com.brasper.platform.accounting.domain.model.aggregates;

import static org.assertj.core.api.Assertions.*;
import com.brasper.platform.accounting.domain.model.commands.*;
import com.brasper.platform.accounting.domain.model.valueobjects.*;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class JournalEntryTest {
    private static final LocalDate DATE = LocalDate.of(2026, 11, 30);
    private Money money(long amount) { return new Money(BigDecimal.valueOf(amount), Currency.PEN); }
    private JournalLineData line(long debit, long credit) {
        return new JournalLineData(new AccountCode("10"), money(debit), money(credit), 42L);
    }
    private JournalEntry entry(JournalLineData... lines) {
        return new JournalEntry(new RegisterJournalEntryCommand(DATE, "x".repeat(300), EntrySource.MANUAL, null, Arrays.asList(lines)));
    }
    private void identify(JournalEntry entry) throws Exception {
        var field = JournalEntry.class.getDeclaredField("id"); field.setAccessible(true); field.set(entry, 7L);
    }
    @Test void balancesTotalsAndProtectsLines() {
        JournalEntry entry = entry(line(100, 0), line(0, 40), line(0, 60));
        assertThat(entry.totalDebit()).isEqualTo(money(100)).isEqualTo(entry.totalCredit());
        assertThat(entry.getPeriod()).isEqualTo(new Period(2026, 11));
        assertThat(entry.getLines()).hasSize(3).allSatisfy(l -> assertThat(l.getJournalEntry()).isSameAs(entry));
        assertThatThrownBy(() -> entry.getLines().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
    @Test void rejectsInvalidLinesAndUnbalancedEntries() {
        assertThatThrownBy(() -> entry()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> entry(line(1, 0))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> entry(line(1, 0), line(0, 2))).isInstanceOf(IllegalArgumentException.class);
        for (JournalLineData invalid : Arrays.asList(null, line(0, 0), line(1, 1), line(-1, 0), line(0, -1),
                line(1, -1), line(-1, 1), line(1_000_000_000_000L, 0), line(0, 1_000_000_000_000L),
                new JournalLineData(null, money(1), money(0), null),
                new JournalLineData(new AccountCode("10"), null, money(0), null),
                new JournalLineData(new AccountCode("10"), money(0), null, null),
                new JournalLineData(new AccountCode("10"), money(1), Money.zero(Currency.USD), null),
                new JournalLineData(new AccountCode("10"), Money.zero(Currency.USD), new Money(BigDecimal.ONE, Currency.USD), null))) {
            assertThatThrownBy(() -> entry(line(1, 0), invalid)).isInstanceOf(IllegalArgumentException.class);
        }
    }
    @Test void rejectsMissingMetadataAndLines() {
        for (String description : Arrays.asList(null, " ", "x".repeat(301))) {
            assertThatThrownBy(() -> new JournalEntry(new RegisterJournalEntryCommand(DATE, description, EntrySource.MANUAL,
                    null, List.of(line(1, 0), line(0, 1))))).isInstanceOf(IllegalArgumentException.class);
        }
        for (RegisterJournalEntryCommand command : Arrays.asList(null,
                new RegisterJournalEntryCommand(null, "entry", EntrySource.MANUAL, null, List.of(line(1, 0), line(0, 1))),
                new RegisterJournalEntryCommand(DATE, "entry", null, null, List.of(line(1, 0), line(0, 1))),
                new RegisterJournalEntryCommand(DATE, "entry", EntrySource.MANUAL, null, null))) {
            assertThatThrownBy(() -> new JournalEntry(command)).isInstanceOf(IllegalArgumentException.class);
        }
    }
    @Test void reversesOnceAndOnlyPersistedOriginals() throws Exception {
        JournalEntry original = entry(line(100, 0), line(0, 100));
        assertThatThrownBy(() -> original.reverse(DATE)).isInstanceOf(IllegalStateException.class);
        identify(original);
        assertThatThrownBy(() -> original.reverse(null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(original.isReversed()).isFalse();
        JournalEntry reversal = original.reverse(DATE.plusDays(1));
        assertThat(original.isReversed()).isTrue();
        assertThat(reversal).extracting("id", "source", "sourceId", "reversalOfId", "reversed", "entryDate", "period")
                .containsExactly(null, EntrySource.REVERSAL, 7L, 7L, false, DATE.plusDays(1), new Period(2026, 12));
        assertThat(reversal.getDescription()).isEqualTo(("Reversal of: " + original.getDescription()).substring(0, 300));
        for (int i = 0; i < 2; i++) {
            var before = original.getLines().get(i); var after = reversal.getLines().get(i);
            assertThat(after).extracting("accountCode", "debit", "credit", "projectId", "journalEntry")
                    .containsExactly(before.getAccountCode(), before.getCredit(), before.getDebit(), before.getProjectId(), reversal);
        }
        assertThat(reversal.totalDebit()).isEqualTo(original.totalCredit());
        assertThatThrownBy(() -> original.reverse(DATE)).isInstanceOf(IllegalStateException.class);
        identify(reversal);
        assertThatThrownBy(() -> reversal.reverse(DATE)).isInstanceOf(IllegalStateException.class);
    }
}
