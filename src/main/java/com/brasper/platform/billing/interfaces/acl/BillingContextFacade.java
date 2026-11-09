package com.brasper.platform.billing.interfaces.acl;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The only way Treasury, Accounting and Reporting read Billing. Voided invoices never count.
 * shortcut: amounts of different currencies are added as plain numbers, so the figures are right for a
 * single-currency company; convert them once Accounting exposes exchange rates.
 */
public interface BillingContextFacade {

    /** Total of invoices issued up to the date, minus the payments (certificates included) received up to it. */
    BigDecimal sumOutstandingReceivablesAt(LocalDate date);

    /** IGV of the invoices issued in the month. */
    BigDecimal sumOutputIgvByPeriod(int year, int month);

    BigDecimal sumInvoicedByProject(Long projectId);

    /** Money deposited for the project; retention certificates are not cash, so they are left out. */
    BigDecimal sumCollectedByProject(Long projectId);

    /** Money deposited during the cash-flow week (certificates left out). */
    BigDecimal sumCollectedByWeek(int year, int month, int week);

    /** Collections still expected (scheduled or rescheduled) in the cash-flow week. */
    BigDecimal sumScheduledCollectionsByWeek(int year, int month, int week);
}
