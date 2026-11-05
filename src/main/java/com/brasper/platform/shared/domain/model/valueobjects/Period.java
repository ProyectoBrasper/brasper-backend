package com.brasper.platform.shared.domain.model.valueobjects;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** A calendar month. */
public record Period(int year, int month) {

    public Period {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12: " + month);
        }
        if (year < 1) {
            throw new IllegalArgumentException("Year must be positive: " + year);
        }
    }

    public static Period from(LocalDate date) {
        return new Period(date.getYear(), date.getMonthValue());
    }

    public LocalDate startDate() {
        return LocalDate.of(year, month, 1);
    }

    public LocalDate endDate() {
        return YearMonth.of(year, month).atEndOfMonth();
    }

    public Period next() {
        YearMonth next = YearMonth.of(year, month).plusMonths(1);
        return new Period(next.getYear(), next.getMonthValue());
    }

    public Period previous() {
        YearMonth previous = YearMonth.of(year, month).minusMonths(1);
        return new Period(previous.getYear(), previous.getMonthValue());
    }

    /** The cash-flow weeks of this month: 4 or 5 of them. */
    public List<CashFlowWeek> weeks() {
        List<CashFlowWeek> weeks = new ArrayList<>();
        for (int number = 1; number <= CashFlowWeek.MAX_WEEKS && CashFlowWeek.exists(this, number); number++) {
            weeks.add(new CashFlowWeek(year, month, number));
        }
        return weeks;
    }
}
