package com.brasper.platform.staff.domain.model.entities;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import com.brasper.platform.staff.domain.model.aggregates.FixedCost;
import com.brasper.platform.staff.domain.model.commands.RegisterFixedCostPaymentCommand;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "fixed_cost_payments", schema = "brasper")
public class FixedCostPayment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fixed_cost_id", nullable = false, updatable = false)
    private FixedCost fixedCost;
    @Column(name = "paid_on", nullable = false, updatable = false)
    private LocalDate paidOn;
    @Column(name = "period_year", nullable = false, updatable = false)
    private int periodYear;
    @Column(name = "period_month", nullable = false, updatable = false)
    private int periodMonth;
    @Column(nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 3, updatable = false)
    private Currency currency;
    @Column(name = "bank_account_id", updatable = false)
    private Long bankAccountId;
    @Column(columnDefinition = "text", updatable = false)
    private String notes;

    protected FixedCostPayment() {}
    public FixedCostPayment(FixedCost fixedCost, RegisterFixedCostPaymentCommand command) {
        this.fixedCost = Objects.requireNonNull(fixedCost, "fixedCost");
        paidOn = Objects.requireNonNull(command.paidOn(), "paidOn");
        Period period = Objects.requireNonNull(command.period(), "period");
        periodYear = period.year(); periodMonth = period.month();
        Money money = Objects.requireNonNull(command.amount(), "amount");
        if (!money.isPositive() || money.amount().precision() > 14 || money.currency() != fixedCost.getMonthlyAmount().currency()) {
            throw new IllegalArgumentException("Invalid payment amount or currency");
        }
        amount = money.amount(); currency = money.currency();
        bankAccountId = command.bankAccountId(); notes = command.notes();
    }
    public Period getPeriod() { return new Period(periodYear, periodMonth); }
    public Money getAmount() { return new Money(amount, currency); }
    public Long getId() { return id; }
    public FixedCost getFixedCost() { return fixedCost; }
    public LocalDate getPaidOn() { return paidOn; }
    public Long getBankAccountId() { return bankAccountId; }
    public String getNotes() { return notes; }
}
