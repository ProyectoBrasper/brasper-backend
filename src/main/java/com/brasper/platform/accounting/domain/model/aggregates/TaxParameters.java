package com.brasper.platform.accounting.domain.model.aggregates;

import com.brasper.platform.accounting.domain.model.commands.SetTaxParametersCommand;
import com.brasper.platform.shared.domain.model.valueobjects.Percentage;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "tax_parameters", uniqueConstraints = @UniqueConstraint(
        name = "uk_tax_parameters_effective_from", columnNames = "effective_from"))
public class TaxParameters {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, precision = 14, scale = 4, updatable = false) private BigDecimal igv;
    @Column(nullable = false, precision = 14, scale = 4, updatable = false) private BigDecimal detraction;
    @Column(nullable = false, precision = 14, scale = 4, updatable = false) private BigDecimal retention;
    @Column(name = "income_tax_prepayment", nullable = false, precision = 14, scale = 4, updatable = false)
    private BigDecimal incomeTaxPrepayment;
    @Column(name = "effective_from", nullable = false, updatable = false) private LocalDate effectiveFrom;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false, updatable = false) private Instant updatedAt;

    protected TaxParameters() { }
    public TaxParameters(SetTaxParametersCommand command) {
        TaxRates rates = Objects.requireNonNull(command.rates(), "rates");
        effectiveFrom = Objects.requireNonNull(command.effectiveFrom(), "effectiveFrom");
        igv = rates.igv().value(); detraction = rates.detraction().value();
        retention = rates.retention().value(); incomeTaxPrepayment = rates.incomeTaxPrepayment().value();
    }
    public TaxRates getRates() {
        return new TaxRates(new Percentage(igv), new Percentage(detraction),
                new Percentage(retention), new Percentage(incomeTaxPrepayment));
    }
    public Long getId() { return id; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
}
