package com.brasper.platform.accounting.domain.model.aggregates;

import com.brasper.platform.accounting.domain.model.commands.PrepareTaxReturnCommand;
import com.brasper.platform.accounting.domain.model.valueobjects.TaxReturnStatus;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "tax_returns", uniqueConstraints = @UniqueConstraint(
        name = TaxReturn.PERIOD_UNIQUE_CONSTRAINT, columnNames = {"period_year", "period_month"}))
public class TaxReturn {
    public static final String PERIOD_UNIQUE_CONSTRAINT = "uk_tax_returns_period";
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "period_year", nullable = false, updatable = false) private int periodYear;
    @Column(name = "period_month", nullable = false, updatable = false) private int periodMonth;
    @Column(name = "output_igv", nullable = false, precision = 14, scale = 2, updatable = false) private BigDecimal outputIgv;
    @Column(name = "input_igv", nullable = false, precision = 14, scale = 2, updatable = false) private BigDecimal inputIgv;
    @Column(name = "carried_credit", nullable = false, precision = 14, scale = 2, updatable = false) private BigDecimal carriedCredit;
    @Column(name = "income_tax_prepayment", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal incomeTaxPrepayment;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 3, updatable = false) private Currency currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private TaxReturnStatus status;
    @Column(name = "paid_on") private LocalDate paidOn;
    @Column(name = "bank_account_id") private Long bankAccountId;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected TaxReturn() { }
    public TaxReturn(PrepareTaxReturnCommand command, Money outputIgv, Money inputIgv,
            Money carriedCredit, Money incomeTaxPrepayment) {
        Period period = Objects.requireNonNull(command.period(), "period");
        periodYear = period.year(); periodMonth = period.month();
        currency = Objects.requireNonNull(outputIgv, "outputIgv").currency();
        for (Money amount : new Money[]{outputIgv, inputIgv, carriedCredit, incomeTaxPrepayment}) {
            if (amount == null || amount.amount().signum() < 0 || amount.currency() != currency
                    || amount.amount().precision() > 14) {
                throw new IllegalArgumentException("Amounts must be nonnegative, fit numeric(14,2) and share currency");
            }
        }
        this.outputIgv = outputIgv.amount(); this.inputIgv = inputIgv.amount();
        this.carriedCredit = carriedCredit.amount(); this.incomeTaxPrepayment = incomeTaxPrepayment.amount();
        status = TaxReturnStatus.DRAFT;
    }
    public Money igvPayable() { return new Money(outputIgv.subtract(inputIgv).subtract(carriedCredit).max(BigDecimal.ZERO), currency); }
    public Money nextCarriedCredit() { return new Money(inputIgv.add(carriedCredit).subtract(outputIgv).max(BigDecimal.ZERO), currency); }
    public Money totalPayable() { return igvPayable().add(getIncomeTaxPrepayment()); }
    public void file() {
        if (status != TaxReturnStatus.DRAFT) throw new IllegalStateException("Tax return is not draft");
        status = TaxReturnStatus.FILED;
    }
    public void markAsPaid(LocalDate paidOn, Long bankAccountId) {
        if (status != TaxReturnStatus.FILED) throw new IllegalStateException("Tax return is not filed");
        if (paidOn == null || bankAccountId == null) throw new IllegalArgumentException("Payment date and bank account are required");
        this.paidOn = paidOn; this.bankAccountId = bankAccountId; status = TaxReturnStatus.PAID;
    }
    public Long getId() { return id; }
    public Period getPeriod() { return new Period(periodYear, periodMonth); }
    public Money getOutputIgv() { return new Money(outputIgv, currency); }
    public Money getInputIgv() { return new Money(inputIgv, currency); }
    public Money getCarriedCredit() { return new Money(carriedCredit, currency); }
    public Money getIncomeTaxPrepayment() { return new Money(incomeTaxPrepayment, currency); }
    public Currency getCurrency() { return currency; }
    public TaxReturnStatus getStatus() { return status; }
    public LocalDate getPaidOn() { return paidOn; }
    public Long getBankAccountId() { return bankAccountId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
