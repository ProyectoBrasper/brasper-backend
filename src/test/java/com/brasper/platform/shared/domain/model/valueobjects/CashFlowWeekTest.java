package com.brasper.platform.shared.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CashFlowWeekTest {

    private static LocalDate d(int y, int m, int day) {
        return LocalDate.of(y, m, day);
    }

    @Test
    void monthStartingMondayHasFullFirstWeek() {
        // June 2026 starts on a Monday.
        CashFlowWeek week1 = new CashFlowWeek(2026, 6, 1);

        assertThat(week1.startDate()).isEqualTo(d(2026, 6, 1));
        assertThat(week1.endDate()).isEqualTo(d(2026, 6, 7));
        assertThat(new CashFlowWeek(2026, 6, 5).startDate()).isEqualTo(d(2026, 6, 29));
        assertThat(new CashFlowWeek(2026, 6, 5).endDate()).isEqualTo(d(2026, 6, 30));
    }

    @Test
    void firstWeekIsClippedToTheFirstSunday() {
        // January 2026 starts on a Thursday.
        assertThat(new CashFlowWeek(2026, 1, 1).endDate()).isEqualTo(d(2026, 1, 4));
        assertThat(new CashFlowWeek(2026, 1, 2).startDate()).isEqualTo(d(2026, 1, 5));
        assertThat(new CashFlowWeek(2026, 1, 5).startDate()).isEqualTo(d(2026, 1, 26));
        assertThat(new CashFlowWeek(2026, 1, 5).endDate()).isEqualTo(d(2026, 1, 31));
    }

    @Test
    void monthStartingSundayHasOneDayFirstWeekAndASixthWeek() {
        // November 2026 starts on a Sunday and has 30 days: Sun 1 | 2-8 | 9-15 | 16-22 | 23-29 | Mon 30.
        assertThat(new CashFlowWeek(2026, 11, 1).endDate()).isEqualTo(d(2026, 11, 1));
        CashFlowWeek week5 = new CashFlowWeek(2026, 11, 5);
        CashFlowWeek week6 = new CashFlowWeek(2026, 11, 6);

        assertThat(week5.startDate()).isEqualTo(d(2026, 11, 23));
        assertThat(week5.endDate()).isEqualTo(d(2026, 11, 29));
        assertThat(week6.startDate()).isEqualTo(d(2026, 11, 30));
        assertThat(week6.endDate()).isEqualTo(d(2026, 11, 30));
        assertThat(CashFlowWeek.from(d(2026, 11, 30))).isEqualTo(week6);
        assertThat(new Period(2026, 11).weeks()).hasSize(6);
    }

    @Test
    void fromDateFindsTheWeekAndContainsAgrees() {
        assertThat(CashFlowWeek.from(d(2026, 11, 5))).isEqualTo(new CashFlowWeek(2026, 11, 2));
        assertThat(CashFlowWeek.from(d(2026, 1, 4))).isEqualTo(new CashFlowWeek(2026, 1, 1));
        assertThat(CashFlowWeek.from(d(2026, 1, 5))).isEqualTo(new CashFlowWeek(2026, 1, 2));

        for (LocalDate day = d(2026, 1, 1); !day.isAfter(d(2026, 12, 31)); day = day.plusDays(1)) {
            assertThat(CashFlowWeek.from(day).contains(day)).as(day.toString()).isTrue();
        }
    }

    @Test
    void periodOfTheWeek() {
        assertThat(new CashFlowWeek(2026, 3, 2).period()).isEqualTo(new Period(2026, 3));
    }

    @Test
    void rejectsWeeksThatDoNotExist() {
        // February 2027 starts on a Monday and has exactly 4 weeks.
        assertThatThrownBy(() -> new CashFlowWeek(2027, 2, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CashFlowWeek(2026, 3, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CashFlowWeek(2026, 3, 7)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CashFlowWeek(2026, 13, 1)).isInstanceOf(IllegalArgumentException.class);
    }
}
