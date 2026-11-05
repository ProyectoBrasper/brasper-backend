package com.brasper.platform.shared.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PeriodTest {

    @Test
    void startAndEndDates() {
        assertThat(new Period(2026, 11).startDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(new Period(2026, 11).endDate()).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(new Period(2028, 2).endDate()).isEqualTo(LocalDate.of(2028, 2, 29));
        assertThat(new Period(2026, 2).endDate()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void nextAndPreviousCrossYearBoundaries() {
        assertThat(new Period(2026, 12).next()).isEqualTo(new Period(2027, 1));
        assertThat(new Period(2026, 1).previous()).isEqualTo(new Period(2025, 12));
        assertThat(new Period(2026, 6).next()).isEqualTo(new Period(2026, 7));
    }

    @Test
    void fromDate() {
        assertThat(Period.from(LocalDate.of(2026, 8, 31))).isEqualTo(new Period(2026, 8));
    }

    @Test
    void rejectsInvalidMonthOrYear() {
        assertThatThrownBy(() -> new Period(2026, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Period(2026, 13)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Period(0, 5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void weeksCoverEveryDayOfTheMonthWithoutGaps() {
        for (int month = 1; month <= 12; month++) {
            Period period = new Period(2026, month);
            var weeks = period.weeks();

            assertThat(weeks).hasSizeBetween(4, 6);
            assertThat(weeks.getFirst().startDate()).isEqualTo(period.startDate());
            assertThat(weeks.getLast().endDate()).isEqualTo(period.endDate());
            for (int i = 1; i < weeks.size(); i++) {
                assertThat(weeks.get(i).startDate()).isEqualTo(weeks.get(i - 1).endDate().plusDays(1));
            }
        }
    }

    @Test
    void februaryStartingOnMondayHasFourWeeks() {
        assertThat(new Period(2027, 2).weeks()).hasSize(4);
    }
}
