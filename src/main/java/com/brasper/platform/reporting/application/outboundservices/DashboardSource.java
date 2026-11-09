package com.brasper.platform.reporting.application.outboundservices;

import java.math.BigDecimal;
import java.time.LocalDate;

/** The balances the dashboard needs from Treasury and Billing. */
public interface DashboardSource {

    BigDecimal cashBalanceAt(LocalDate date);

    BigDecimal receivablesOutstandingAt(LocalDate date);

    /** What is owed now on payables that are not fully paid; there is no history to ask for another date. */
    BigDecimal payablesOutstanding();
}
