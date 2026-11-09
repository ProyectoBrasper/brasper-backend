package com.brasper.platform.treasury.domain.model.entities;

import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.treasury.domain.model.aggregates.Loan;
import com.brasper.platform.treasury.domain.model.commands.ScheduleLoanInstallmentCommand;
import com.brasper.platform.treasury.domain.model.commands.PayLoanInstallmentCommand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "loan_installments")
public class LoanInstallment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false, updatable = false) private Loan loan;
    @Column(name = "due_on", nullable = false, updatable = false) private LocalDate dueOn;
    @Column(name = "due_year", nullable = false, updatable = false) private int dueYear;
    @Column(name = "due_month", nullable = false, updatable = false) private int dueMonth;
    @Column(name = "due_week", nullable = false, updatable = false) private int dueWeek;
    @Column(nullable = false, precision = 14, scale = 2, updatable = false) private BigDecimal principal;
    @Column(nullable = false, precision = 14, scale = 2, updatable = false) private BigDecimal interest;
    @Column(nullable = false) private boolean paid;
    @Column(name = "paid_on") private LocalDate paidOn;
    @Column(name = "bank_account_id") private Long bankAccountId;

    protected LoanInstallment() { }
    public LoanInstallment(Loan loan, ScheduleLoanInstallmentCommand command) {
        this.loan = Objects.requireNonNull(loan, "loan");
        Money capital = Objects.requireNonNull(command.principal(), "principal");
        Money charge = Objects.requireNonNull(command.interest(), "interest");
        if (capital.currency() != loan.getPrincipal().currency() || charge.currency() != capital.currency()
                || !capital.isPositive() || charge.amount().signum() < 0
                || capital.amount().precision() > 14 || charge.amount().precision() > 14) {
            throw new IllegalArgumentException("Installment amounts must fit numeric(14,2), match currency, with principal > 0 and interest >= 0");
        }
        dueOn = Objects.requireNonNull(command.dueOn(), "dueOn");
        CashFlowWeek week = CashFlowWeek.from(dueOn);
        dueYear = week.year(); dueMonth = week.month(); dueWeek = week.number();
        principal = capital.amount(); interest = charge.amount();
    }
    public void pay(PayLoanInstallmentCommand command) {
        if (paid) throw new IllegalStateException("Installment is already paid");
        Objects.requireNonNull(command.paidOn(), "paidOn");
        Objects.requireNonNull(command.bankAccountId(), "bankAccountId");
        paidOn = command.paidOn(); bankAccountId = command.bankAccountId(); paid = true;
    }
    public Long getId() { return id; }
    public LocalDate getDueOn() { return dueOn; }
    public CashFlowWeek getDueWeek() { return new CashFlowWeek(dueYear, dueMonth, dueWeek); }
    public Money getPrincipal() { return new Money(principal, loan.getPrincipal().currency()); }
    public Money getInterest() { return new Money(interest, loan.getPrincipal().currency()); }
    public Money total() { return getPrincipal().add(getInterest()); }
    public boolean isPaid() { return paid; }
    public LocalDate getPaidOn() { return paidOn; }
    public Long getBankAccountId() { return bankAccountId; }
}
