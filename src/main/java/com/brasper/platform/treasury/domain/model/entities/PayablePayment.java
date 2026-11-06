package com.brasper.platform.treasury.domain.model.entities;

import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.treasury.domain.model.aggregates.Payable;
import com.brasper.platform.treasury.domain.model.commands.RegisterPayablePaymentCommand;
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
@Table(name = "payable_payments")
public class PayablePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payable_id", nullable = false, updatable = false)
    private Payable payable;
    @Column(name = "paid_on", nullable = false, updatable = false)
    private LocalDate paidOn;
    @Column(nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal amount;
    @Column(name = "bank_account_id", nullable = false, updatable = false)
    private Long bankAccountId;

    protected PayablePayment() {
    }

    public PayablePayment(Payable payable, RegisterPayablePaymentCommand command) {
        this.payable = Objects.requireNonNull(payable, "payable");
        this.paidOn = Objects.requireNonNull(command.paidOn(), "paidOn");
        this.amount = Objects.requireNonNull(command.amount(), "amount").amount();
        this.bankAccountId = Objects.requireNonNull(command.bankAccountId(), "bankAccountId");
    }

    public Long getId() { return id; }
    public LocalDate getPaidOn() { return paidOn; }
    public Money getAmount() { return new Money(amount, payable.getTotalAmount().currency()); }
    public Long getBankAccountId() { return bankAccountId; }
}
