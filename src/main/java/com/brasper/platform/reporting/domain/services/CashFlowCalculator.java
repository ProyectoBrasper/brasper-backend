package com.brasper.platform.reporting.domain.services;

import com.brasper.platform.reporting.domain.model.readmodels.CashFlowLine;
import com.brasper.platform.reporting.domain.model.readmodels.CashFlowReport;
import com.brasper.platform.reporting.domain.model.valueobjects.CashFlowConcept;
import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Chains the weeks: the first opening balance is the real cash the day before, and each next week opens with the
 * previous closing. A week that ended before today shows what happened; the current and later weeks show what is
 * scheduled. shortcut: a range that starts in the future still opens with today's cash, the weeks in between
 * are not projected.
 */
public final class CashFlowCalculator {

    private CashFlowCalculator() { }

    public static CashFlowReport buildWeekly(List<CashFlowWeek> weeks, CashFlowSource source, LocalDate today) {
        if (weeks.isEmpty()) {
            throw new IllegalArgumentException("At least one week is required");
        }
        BigDecimal opening = source.cashBalanceAt(weeks.getFirst().startDate().minusDays(1));
        List<CashFlowLine> lines = new ArrayList<>();
        for (CashFlowWeek week : weeks) {
            CashFlowLine line = weekLine(week, opening, source, today);
            lines.add(line);
            opening = line.closingBalance();
        }
        return new CashFlowReport(weeks.getFirst().startDate(), weeks.getLast().endDate(), lines);
    }

    /** One line per month, each the sum of the weeks of that month. */
    public static CashFlowReport buildMonthly(List<Period> periods, CashFlowSource source, LocalDate today) {
        if (periods.isEmpty()) {
            throw new IllegalArgumentException("At least one month is required");
        }
        BigDecimal opening = source.cashBalanceAt(periods.getFirst().startDate().minusDays(1));
        List<CashFlowLine> lines = new ArrayList<>();
        for (Period period : periods) {
            Map<CashFlowConcept, BigDecimal> inflows = new EnumMap<>(CashFlowConcept.class);
            Map<CashFlowConcept, BigDecimal> outflows = new EnumMap<>(CashFlowConcept.class);
            boolean projected = false;
            for (CashFlowWeek week : period.weeks()) {
                CashFlowLine weekLine = weekLine(week, BigDecimal.ZERO, source, today);
                weekLine.inflows().forEach((concept, amount) -> inflows.merge(concept, amount, BigDecimal::add));
                weekLine.outflows().forEach((concept, amount) -> outflows.merge(concept, amount, BigDecimal::add));
                projected |= weekLine.projected();
            }
            CashFlowLine line = new CashFlowLine("%d-%02d".formatted(period.year(), period.month()),
                    period.startDate(), period.endDate(), projected, opening, inflows, outflows);
            lines.add(line);
            opening = line.closingBalance();
        }
        return new CashFlowReport(periods.getFirst().startDate(), periods.getLast().endDate(), lines);
    }

    private static CashFlowLine weekLine(CashFlowWeek week, BigDecimal opening, CashFlowSource source, LocalDate today) {
        boolean projected = !week.endDate().isBefore(today);
        return new CashFlowLine("%d-%02d Week %02d".formatted(week.year(), week.month(), week.number()),
                week.startDate(), week.endDate(), projected, opening,
                projected ? source.projectedInflows(week) : source.actualInflows(week),
                projected ? source.projectedOutflows(week) : source.actualOutflows(week));
    }
}
