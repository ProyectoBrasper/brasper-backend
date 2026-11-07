package com.brasper.platform.treasury.application.queryservices;

import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.PaymentStatus;
import com.brasper.platform.treasury.domain.model.aggregates.BankAccount;
import com.brasper.platform.treasury.domain.model.aggregates.Loan;
import com.brasper.platform.treasury.domain.model.aggregates.Payable;
import com.brasper.platform.treasury.domain.model.entities.CashMovement;
import com.brasper.platform.treasury.domain.model.entities.LoanInstallment;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementDirection;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementSource;
import com.brasper.platform.treasury.domain.repositories.BankAccountRepository;
import com.brasper.platform.treasury.domain.repositories.LoanRepository;
import com.brasper.platform.treasury.domain.repositories.PayableRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cash figures for Reporting. shortcut: amounts of different currencies are added as plain numbers and everything
 * is summed in memory; move to SQL sums when the movement history grows.
 */
@Service
@Transactional(readOnly = true)
public class CashFigures {

    private final BankAccountRepository accounts;
    private final PayableRepository payables;
    private final LoanRepository loans;

    public CashFigures(BankAccountRepository accounts, PayableRepository payables, LoanRepository loans) {
        this.accounts = accounts;
        this.payables = payables;
        this.loans = loans;
    }

    /** Opening balances of the accounts open on the date plus every movement up to and including it. */
    public BigDecimal balanceAt(LocalDate date) {
        BigDecimal total = BigDecimal.ZERO;
        for (BankAccount account : accounts.findAll()) {
            if (account.getOpenedOn().isAfter(date)) {
                continue;
            }
            total = total.add(account.getOpeningBalance().amount());
            for (CashMovement movement : account.getMovements()) {
                if (!movement.getOccurredOn().isAfter(date)) {
                    total = total.add(signed(movement));
                }
            }
        }
        return total;
    }

    /** Money in and out between the dates (both included), keyed {@code INFLOW:SOURCE} or {@code OUTFLOW:SOURCE}. */
    public Map<String, BigDecimal> movementsBetween(LocalDate from, LocalDate to) {
        Map<String, BigDecimal> totals = new TreeMap<>();
        for (BankAccount account : accounts.findAll()) {
            for (CashMovement movement : account.getMovements()) {
                boolean inRange = !movement.getOccurredOn().isBefore(from) && !movement.getOccurredOn().isAfter(to);
                // A transfer only moves money between the company's own accounts.
                if (inRange && movement.getSource() != MovementSource.TRANSFER) {
                    totals.merge(movement.getDirection() + ":" + movement.getSource(), movement.getAmount().amount(),
                            BigDecimal::add);
                }
            }
        }
        return totals;
    }

    /** What is still owed on the payables scheduled for the week. */
    public BigDecimal scheduledPayables(CashFlowWeek week) {
        return payables.findAll().stream()
                .filter(payable -> payable.getStatus() != PaymentStatus.PAID
                        && payable.getScheduledWeek().equals(week))
                .map(Payable::balance).map(money -> money.amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Unpaid installments due in the week, keyed by loan type ({@code RECEIVED}: to pay, {@code GRANTED}: to collect). */
    public Map<String, BigDecimal> scheduledInstallments(CashFlowWeek week) {
        Map<String, BigDecimal> totals = new TreeMap<>();
        for (Loan loan : loans.findAll()) {
            for (LoanInstallment installment : loan.getInstallments()) {
                if (!installment.isPaid() && installment.getDueWeek().equals(week)) {
                    totals.merge(loan.getType().name(), installment.total().amount(), BigDecimal::add);
                }
            }
        }
        return totals;
    }

    private static BigDecimal signed(CashMovement movement) {
        return movement.getDirection() == MovementDirection.INFLOW
                ? movement.getAmount().amount() : movement.getAmount().amount().negate();
    }
}
