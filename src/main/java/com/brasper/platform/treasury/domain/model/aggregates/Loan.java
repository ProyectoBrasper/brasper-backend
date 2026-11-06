package com.brasper.platform.treasury.domain.model.aggregates;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.PaymentStatus;
import com.brasper.platform.shared.domain.model.valueobjects.Percentage;
import com.brasper.platform.treasury.domain.model.commands.RegisterLoanCommand;
import com.brasper.platform.treasury.domain.model.commands.ScheduleLoanInstallmentCommand;
import com.brasper.platform.treasury.domain.model.commands.PayLoanInstallmentCommand;
import com.brasper.platform.treasury.domain.model.entities.LoanInstallment;
import com.brasper.platform.treasury.domain.model.valueobjects.LoanType;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "loans")
public class Loan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20, updatable = false) private LoanType type;
    @Column(nullable = false, length = 200, updatable = false) private String counterparty;
    @Column(nullable = false, precision = 14, scale = 2, updatable = false) private BigDecimal principal;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 3, updatable = false) private Currency currency;
    @Column(name = "interest_rate", nullable = false, columnDefinition = "numeric", updatable = false) private BigDecimal interestRate;
    @Column(name = "disbursed_on", nullable = false, updatable = false) private LocalDate disbursedOn;
    @Column(name = "bank_account_id", nullable = false, updatable = false) private Long bankAccountId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentStatus status = PaymentStatus.PENDING;
    // shortcut: eager installments for small histories, use paged queries when histories grow.
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<LoanInstallment> installments = new ArrayList<>();
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Loan() { }
    public Loan(RegisterLoanCommand command) {
        if (command.counterparty() == null || command.counterparty().isBlank() || command.counterparty().strip().length() > 200) {
            throw new IllegalArgumentException("Counterparty is required and must not exceed 200 characters");
        }
        Money amount = Objects.requireNonNull(command.principal(), "principal");
        if (!amount.isPositive() || amount.amount().precision() > 14) {
            throw new IllegalArgumentException("Principal must be positive and fit numeric(14,2)");
        }
        counterparty = command.counterparty().strip();
        type = Objects.requireNonNull(command.type(), "type");
        principal = amount.amount();
        currency = amount.currency();
        interestRate = Objects.requireNonNull(command.interestRate(), "interestRate").value();
        disbursedOn = Objects.requireNonNull(command.disbursedOn(), "disbursedOn");
        bankAccountId = Objects.requireNonNull(command.bankAccountId(), "bankAccountId");
    }
    public LoanInstallment scheduleInstallment(ScheduleLoanInstallmentCommand command) {
        LoanInstallment installment = new LoanInstallment(this, command);
        Money scheduled = installments.stream().map(LoanInstallment::getPrincipal).reduce(Money.zero(currency), Money::add);
        if (scheduled.add(installment.getPrincipal()).amount().compareTo(principal) > 0) {
            throw new IllegalStateException("Installments exceed the loan principal");
        }
        installments.add(installment);
        updatedAt = Instant.now();
        return installment;
    }
    public LoanInstallment payInstallment(PayLoanInstallmentCommand command) {
        LoanInstallment installment = installments.stream()
                .filter(item -> command.installmentId() != null && command.installmentId().equals(item.getId()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Installment does not belong to loan"));
        if (!Objects.equals(id, command.loanId())) throw new IllegalArgumentException("Loan id mismatch");
        installment.pay(command);
        status = outstandingBalance().isPositive() ? PaymentStatus.PARTIALLY_PAID : PaymentStatus.PAID;
        updatedAt = Instant.now();
        return installment;
    }
    public Money totalInterest() {
        return installments.stream().map(LoanInstallment::getInterest).reduce(Money.zero(currency), Money::add);
    }
    public Money outstandingBalance() {
        return getPrincipal().subtract(installments.stream().filter(LoanInstallment::isPaid)
                .map(LoanInstallment::getPrincipal).reduce(Money.zero(currency), Money::add));
    }
    @PrePersist
    void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public LoanType getType() { return type; }
    public String getCounterparty() { return counterparty; }
    public Money getPrincipal() { return new Money(principal, currency); }
    public Percentage getInterestRate() { return new Percentage(interestRate); }
    public LocalDate getDisbursedOn() { return disbursedOn; }
    public Long getBankAccountId() { return bankAccountId; }
    public PaymentStatus getStatus() { return status; }
    public List<LoanInstallment> getInstallments() { return List.copyOf(installments); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
