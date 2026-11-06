package com.brasper.platform.accounting.domain.model.aggregates;

import com.brasper.platform.accounting.domain.model.commands.RegisterFixedAssetCommand;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.TaxBreakdown;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Entity
@Table(name = "fixed_assets")
public class FixedAsset {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 300, updatable = false) private String description;
    @Column(name = "acquired_on", nullable = false, updatable = false) private LocalDate acquiredOn;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 3, updatable = false) private Currency currency;
    @Column(name = "taxable_base", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal taxableBase;
    @Column(nullable = false, precision = 14, scale = 2, updatable = false) private BigDecimal igv;
    @Column(nullable = false, precision = 14, scale = 2, updatable = false) private BigDecimal total;
    @Column(name = "useful_life_months", nullable = false, updatable = false) private int usefulLifeMonths;
    @Column(name = "expense_id", updatable = false) private Long expenseId;
    @Column(name = "disposed_on") private LocalDate disposedOn;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected FixedAsset() { }
    public FixedAsset(RegisterFixedAssetCommand command, TaxRates rates) {
        if (command.description() == null || command.description().isBlank() || command.description().strip().length() > 300) {
            throw new IllegalArgumentException("Description is required and must not exceed 300 characters");
        }
        if (command.usefulLifeMonths() < 1 || command.usefulLifeMonths() > 600) {
            throw new IllegalArgumentException("Useful life must be between 1 and 600 months");
        }
        Money base = Objects.requireNonNull(command.taxableBase(), "taxableBase");
        TaxBreakdown amounts = TaxBreakdown.fromTaxableBase(base, Objects.requireNonNull(rates, "rates").igv());
        if (!base.isPositive() || base.amount().precision() > 14 || amounts.total().amount().precision() > 14) {
            throw new IllegalArgumentException("Amounts must be positive and fit numeric(14,2)");
        }
        description = command.description().strip();
        acquiredOn = Objects.requireNonNull(command.acquiredOn(), "acquiredOn");
        taxableBase = base.amount(); igv = amounts.igv().amount(); total = amounts.total().amount();
        currency = base.currency(); usefulLifeMonths = command.usefulLifeMonths(); expenseId = command.expenseId();
    }
    public Money monthlyDepreciation() {
        return new Money(taxableBase.divide(BigDecimal.valueOf(usefulLifeMonths), 2, RoundingMode.HALF_UP), currency);
    }
    public Money accumulatedDepreciationAt(LocalDate date) {
        Objects.requireNonNull(date, "date");
        LocalDate end = disposedOn != null && disposedOn.isBefore(date) ? disposedOn : date;
        long months = Math.max(0, Math.min(usefulLifeMonths,
                ChronoUnit.MONTHS.between(YearMonth.from(acquiredOn), YearMonth.from(end))));
        BigDecimal accumulated = months == usefulLifeMonths ? taxableBase
                : monthlyDepreciation().amount().multiply(BigDecimal.valueOf(months)).min(taxableBase);
        return new Money(accumulated, currency);
    }
    public Money bookValueAt(LocalDate date) {
        return new Money(taxableBase, currency).subtract(accumulatedDepreciationAt(date));
    }
    public void dispose(LocalDate date) {
        if (disposedOn != null) throw new IllegalStateException("Asset is already disposed");
        Objects.requireNonNull(date, "disposedOn");
        if (date.isBefore(acquiredOn)) throw new IllegalArgumentException("Disposal cannot precede acquisition");
        disposedOn = date;
        updatedAt = Instant.now();
    }
    public Long getId() { return id; }
    public String getDescription() { return description; }
    public LocalDate getAcquiredOn() { return acquiredOn; }
    public TaxBreakdown getAmounts() {
        return new TaxBreakdown(new Money(taxableBase, currency), new Money(igv, currency), new Money(total, currency));
    }
    public int getUsefulLifeMonths() { return usefulLifeMonths; }
    public Long getExpenseId() { return expenseId; }
    public LocalDate getDisposedOn() { return disposedOn; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
