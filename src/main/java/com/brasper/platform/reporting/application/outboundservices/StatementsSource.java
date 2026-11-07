package com.brasper.platform.reporting.application.outboundservices;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** What the financial statements need from Accounting. Both maps are keyed by account code, in PEN. */
public interface StatementsSource {

    /** Movement per account over the dates, both included. Revenue and equity accounts come out positive. */
    Map<String, BigDecimal> movementsBetween(LocalDate from, LocalDate to);

    /** Balance per account up to and including the date, signed by the account's normal side. */
    Map<String, BigDecimal> balancesAt(LocalDate date);
}
