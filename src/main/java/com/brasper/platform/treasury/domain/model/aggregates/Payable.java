package com.brasper.platform.treasury.domain.model.aggregates;

import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.PaymentStatus;
import com.brasper.platform.treasury.domain.model.commands.CreatePayableCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterPayablePaymentCommand;
import com.brasper.platform.treasury.domain.model.entities.PayablePayment;
import com.brasper.platform.treasury.domain.model.valueobjects.PayableType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "payables")
public class Payable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 500, updatable = false)
    private String concept;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private PayableType type;
    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal totalAmount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3, updatable = false)
    private Currency currency;
    @Column(name = "supplier_id", updatable = false)
    private Long supplierId;
    @Column(name = "project_id", updatable = false)
    private Long projectId;
    @Column(name = "expense_id", updatable = false)
    private Long expenseId;
    @Column(name = "scheduled_year", nullable = false)
    private int scheduledYear;
    @Column(name = "scheduled_month", nullable = false)
    private int scheduledMonth;
    @Column(name = "scheduled_week", nullable = false)
    private int scheduledWeek;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;
    // shortcut: eager payments for small histories, use paged queries when histories grow.
    @OneToMany(mappedBy = "payable", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<PayablePayment> payments = new ArrayList<>();
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payable() {
    }

    public Payable(CreatePayableCommand command) {
        if (command.concept() == null || command.concept().isBlank() || command.concept().strip().length() > 500) {
            throw new IllegalArgumentException("Concept is required and must not exceed 500 characters");
        }
        Money total = Objects.requireNonNull(command.totalAmount(), "totalAmount");
        if (!total.isPositive() || total.amount().precision() > 14) {
            throw new IllegalArgumentException("Total must be positive and fit numeric(14,2)");
        }
        this.concept = command.concept().strip();
        this.type = Objects.requireNonNull(command.type(), "type");
        this.totalAmount = total.amount();
        this.currency = total.currency();
        this.supplierId = command.supplierId();
        this.projectId = command.projectId();
        this.expenseId = command.expenseId();
        CashFlowWeek week = Objects.requireNonNull(command.scheduledWeek(), "scheduledWeek");
        this.scheduledYear = week.year();
        this.scheduledMonth = week.month();
        this.scheduledWeek = week.number();
    }

    public PayablePayment registerPayment(RegisterPayablePaymentCommand command) {
        if (status == PaymentStatus.PAID) {
            throw new IllegalStateException("The payable is already paid");
        }
        Money amount = Objects.requireNonNull(command.amount(), "amount");
        if (amount.currency() != currency || !amount.isPositive()) {
            throw new IllegalArgumentException("Payment must be positive and in the payable's currency");
        }
        if (amount.amount().compareTo(balance().amount()) > 0) {
            throw new IllegalStateException("Payment exceeds the outstanding balance");
        }
        PayablePayment payment = new PayablePayment(this, command);
        payments.add(payment);
        status = balance().isPositive() ? PaymentStatus.PARTIALLY_PAID : PaymentStatus.PAID;
        updatedAt = Instant.now();
        return payment;
    }

    public Money paidAmount() {
        return payments.stream().map(PayablePayment::getAmount).reduce(Money.zero(currency), Money::add);
    }

    public Money balance() { return getTotalAmount().subtract(paidAmount()); }

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getConcept() { return concept; }
    public PayableType getType() { return type; }
    public Money getTotalAmount() { return new Money(totalAmount, currency); }
    public Long getSupplierId() { return supplierId; }
    public Long getProjectId() { return projectId; }
    public Long getExpenseId() { return expenseId; }
    public CashFlowWeek getScheduledWeek() { return new CashFlowWeek(scheduledYear, scheduledMonth, scheduledWeek); }
    public PaymentStatus getStatus() { return status; }
    public List<PayablePayment> getPayments() { return List.copyOf(payments); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
