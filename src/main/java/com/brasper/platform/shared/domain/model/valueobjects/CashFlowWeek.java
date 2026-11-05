package com.brasper.platform.shared.domain.model.valueobjects;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * "Semana 01..06" of a month. Weeks run Monday to Sunday, clipped to the month: week 1 ends on the
 * first Sunday and the last week ends on the last day of the month. Most months have 5 weeks, some
 * 4 (February starting on a Monday) and some 6 (e.g. November 2026, where week 6 is only the 30th).
 */
public record CashFlowWeek(int year, int month, int number) {

    static final int MAX_WEEKS = 6;

    public CashFlowWeek {
        Period period = new Period(year, month);
        if (!exists(period, number)) {
            throw new IllegalArgumentException("No week %d in %d-%02d".formatted(number, year, month));
        }
    }

    public static CashFlowWeek from(LocalDate date) {
        Period period = Period.from(date);
        LocalDate secondWeekStart = firstSunday(period).plusDays(1);
        int number = date.isBefore(secondWeekStart)
                ? 1
                : 2 + (int) ChronoUnit.WEEKS.between(secondWeekStart, date);
        return new CashFlowWeek(period.year(), period.month(), number);
    }

    public Period period() {
        return new Period(year, month);
    }

    public LocalDate startDate() {
        return number == 1 ? period().startDate() : startOf(period(), number);
    }

    public LocalDate endDate() {
        Period period = period();
        LocalDate end = number == 1 ? firstSunday(period) : startOf(period, number).plusDays(6);
        return end.isAfter(period.endDate()) ? period.endDate() : end;
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate()) && !date.isAfter(endDate());
    }

    static boolean exists(Period period, int number) {
        if (number < 1 || number > MAX_WEEKS) {
            return false;
        }
        return number == 1 || !startOf(period, number).isAfter(period.endDate());
    }

    private static LocalDate firstSunday(Period period) {
        return period.startDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }

    /** Start of week n (n >= 2): the Monday after the first Sunday, plus whole weeks. */
    private static LocalDate startOf(Period period, int number) {
        return firstSunday(period).plusDays(1).plusWeeks(number - 2L);
    }
}
