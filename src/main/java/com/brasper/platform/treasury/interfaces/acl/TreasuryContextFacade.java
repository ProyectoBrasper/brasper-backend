package com.brasper.platform.treasury.interfaces.acl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/** The only way other contexts ask Treasury about bank accounts and partners. They keep the id, nothing else. */
public interface TreasuryContextFacade {

    boolean existsBankAccountById(Long bankAccountId);

    /** The {@code AccountType} name, e.g. {@code CHECKING}; empty when the account does not exist. */
    Optional<String> fetchBankAccountType(Long bankAccountId);

    boolean existsPartnerById(Long partnerId);

    /** Sum of the accounts' opening balances and movements up to and including the date. */
    BigDecimal fetchCashBalanceAt(LocalDate date);

    /**
     * Cash movements between the dates (both included), keyed {@code INFLOW:SOURCE} or {@code OUTFLOW:SOURCE} with the
     * {@code MovementSource} name. Transfers between the company's own accounts are left out.
     */
    Map<String, BigDecimal> fetchMovementsBetween(LocalDate from, LocalDate to);

    /** What is still owed on every payable that is not fully paid, whatever its scheduled week. */
    BigDecimal sumOutstandingPayables();

    /** What is still owed on the payables scheduled for the cash-flow week. */
    BigDecimal sumScheduledPayablesByWeek(int year, int month, int week);

    /** Unpaid loan installments due in the week, keyed by {@code LoanType} name: RECEIVED to pay, GRANTED to collect. */
    Map<String, BigDecimal> sumScheduledInstallmentsByWeek(int year, int month, int week);
}
