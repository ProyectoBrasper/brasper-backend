package com.brasper.platform.reporting.application.queryservices;

import com.brasper.platform.reporting.domain.model.readmodels.CashFlowReport;
import com.brasper.platform.reporting.domain.services.CashFlowCalculator;
import com.brasper.platform.reporting.domain.services.CashFlowSource;
import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** {@code today} is a parameter so that what is real and what is scheduled can be tested on fixed dates. */
@Service
public class CashFlowQueryService {

    static final int MAX_WEEKS = 52;
    static final int MAX_MONTHS = 24;

    private final CashFlowSource source;

    public CashFlowQueryService(CashFlowSource source) {
        this.source = source;
    }

    /** The weeks of one month. */
    public CashFlowReport weekly(Period period, LocalDate today) {
        return CashFlowCalculator.buildWeekly(period.weeks(), source, today);
    }

    /** One line per month from {@code from} to {@code to}, both included. */
    public CashFlowReport monthly(Period from, Period to, LocalDate today) {
        List<Period> periods = new ArrayList<>();
        for (Period period = from; !isAfter(period, to); period = period.next()) {
            periods.add(period);
            if (periods.size() > MAX_MONTHS) {
                throw new IllegalArgumentException("At most %d months can be requested".formatted(MAX_MONTHS));
            }
        }
        if (periods.isEmpty()) {
            throw new IllegalArgumentException("The first month must not come after the last month");
        }
        return CashFlowCalculator.buildMonthly(periods, source, today);
    }

    /** The {@code weeks} consecutive weeks starting with the one that contains {@code from}. */
    public CashFlowReport projection(LocalDate from, int weeks, LocalDate today) {
        if (weeks < 1 || weeks > MAX_WEEKS) {
            throw new IllegalArgumentException("Weeks must be between 1 and %d".formatted(MAX_WEEKS));
        }
        List<CashFlowWeek> list = new ArrayList<>();
        CashFlowWeek week = CashFlowWeek.from(from);
        for (int i = 0; i < weeks; i++) {
            list.add(week);
            week = CashFlowWeek.from(week.endDate().plusDays(1));
        }
        return CashFlowCalculator.buildWeekly(list, source, today);
    }

    private static boolean isAfter(Period a, Period b) {
        return a.year() * 12 + a.month() > b.year() * 12 + b.month();
    }
}
