package com.brasper.platform.billing.domain.model.aggregates;

import com.brasper.platform.billing.domain.model.commands.IssueInvoiceCommand;
import com.brasper.platform.billing.domain.model.commands.RegisterInvoicePaymentCommand;
import com.brasper.platform.billing.domain.model.entities.InvoicePayment;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceNumber;
import com.brasper.platform.billing.domain.model.valueobjects.InvoicePaymentType;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceStatus;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.TaxBreakdown;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
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

/**
 * An issued invoice and root of its payments. Tax amounts are fixed at issue time with the rates in force then.
 * Collected so far and the status follow from the payments.
 */
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 4, updatable = false)
    private String series;

    @Column(nullable = false, updatable = false)
    private int correlative;

    @Column(name = "issued_on", nullable = false, updatable = false)
    private LocalDate issuedOn;

    @Column(name = "purchase_order", length = 50)
    private String purchaseOrder;

    /** Aggregates of other contexts are referenced by id only. */
    @Column(name = "project_id", nullable = false, updatable = false)
    private Long projectId;

    @Column(name = "client_id", nullable = false, updatable = false)
    private Long clientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3, updatable = false)
    private Currency currency;

    @Column(name = "taxable_base", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal taxableBase;

    @Column(nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal igv;

    @Column(nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal total;

    @Column(nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal retention;

    @Column(nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal detraction;

    @Column(name = "has_retention", nullable = false, updatable = false)
    private boolean hasRetention;

    @Column(name = "has_detraction", nullable = false, updatable = false)
    private boolean hasDetraction;

    @Column(name = "self_detraction", nullable = false, updatable = false)
    private boolean selfDetraction;

    @Column(nullable = false, updatable = false)
    private boolean advance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Column(length = 1000)
    private String notes;

    /** shortcut: loaded eagerly (a few payments per invoice), so resources can be built outside a transaction. */
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<InvoicePayment> payments = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Invoice() {
    }

    /** Retention is {@code total x rate}; detraction is {@code total x rate} rounded to whole units (SUNAT). */
    public Invoice(IssueInvoiceCommand command, Long clientId, TaxRates rates) {
        Objects.requireNonNull(rates, "rates");
        Money base = Objects.requireNonNull(command.taxableBase(), "taxableBase");
        if (!base.isPositive()) {
            throw new IllegalArgumentException("Taxable base must be greater than zero");
        }
        if (command.selfDetraction() && !command.hasDetraction()) {
            throw new IllegalArgumentException("Self detraction needs a detraction");
        }
        TaxBreakdown amounts = TaxBreakdown.fromTaxableBase(base, rates.igv());
        this.series = command.number().series();
        this.correlative = command.number().correlative();
        this.issuedOn = Objects.requireNonNull(command.issuedOn(), "issuedOn");
        this.purchaseOrder = blankToNull(command.purchaseOrder());
        this.projectId = Objects.requireNonNull(command.projectId(), "projectId");
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.currency = base.currency();
        this.taxableBase = amounts.taxableBase().amount();
        this.igv = amounts.igv().amount();
        this.total = amounts.total().amount();
        this.hasRetention = command.hasRetention();
        this.hasDetraction = command.hasDetraction();
        this.selfDetraction = command.selfDetraction();
        this.advance = command.advance();
        this.retention = hasRetention ? rates.retention().of(amounts.total()).amount() : BigDecimal.ZERO;
        this.detraction = hasDetraction ? rates.detraction().of(amounts.total()).roundToUnits().amount()
                : BigDecimal.ZERO;
        this.notes = blankToNull(command.notes());
    }

    /** What the client deposits to the company: total minus retention and detraction. */
    public Money netAmount() {
        return getTotal().subtract(getRetention()).subtract(getDetraction());
    }

    /** Sum of every payment, retention certificates included. */
    public Money collectedAmount() {
        return payments.stream().map(InvoicePayment::getAmount).reduce(Money.zero(currency), Money::add);
    }

    public Money outstandingBalance() {
        return getTotal().subtract(collectedAmount());
    }

    /**
     * Adds a payment. A voided invoice takes none, and the total collected cannot pass the invoice total. The status
     * becomes COLLECTED once nothing is outstanding (the Excel mixed "=0" and "<=0").
     */
    public InvoicePayment registerPayment(RegisterInvoicePaymentCommand command) {
        if (status == InvoiceStatus.VOIDED) {
            throw new IllegalStateException("A voided invoice cannot receive payments");
        }
        Money amount = Objects.requireNonNull(command.amount(), "amount");
        if (amount.currency() != currency) {
            throw new IllegalArgumentException("Payment must be in %s but was %s".formatted(currency, amount.currency()));
        }
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Payment must be greater than zero");
        }
        if (command.type() != InvoicePaymentType.RETENTION_CERTIFICATE && command.bankAccountId() == null) {
            throw new IllegalArgumentException("A deposit needs the bank account it went into");
        }
        if (amount.amount().compareTo(outstandingBalance().amount()) > 0) {
            throw new IllegalStateException("Payment %s exceeds the outstanding balance %s"
                    .formatted(amount.amount(), outstandingBalance().amount()));
        }
        InvoicePayment payment = new InvoicePayment(this, command);
        payments.add(payment);
        status = outstandingBalance().isPositive() ? InvoiceStatus.PARTIALLY_COLLECTED : InvoiceStatus.COLLECTED;
        return payment;
    }

    /** Only an invoice with no payments can be voided. */
    public void voidInvoice() {
        if (status == InvoiceStatus.VOIDED) {
            throw new IllegalStateException("The invoice is already voided");
        }
        if (!payments.isEmpty()) {
            throw new IllegalStateException("An invoice with payments cannot be voided");
        }
        status = InvoiceStatus.VOIDED;
    }

    public List<InvoicePayment> getPayments() {
        return List.copyOf(payments);
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public InvoiceNumber getNumber() {
        return new InvoiceNumber(series, correlative);
    }

    public LocalDate getIssuedOn() {
        return issuedOn;
    }

    public String getPurchaseOrder() {
        return purchaseOrder;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getClientId() {
        return clientId;
    }

    public TaxBreakdown getAmounts() {
        return new TaxBreakdown(new Money(taxableBase, currency), new Money(igv, currency), getTotal());
    }

    public Money getTotal() {
        return new Money(total, currency);
    }

    public Money getRetention() {
        return new Money(retention, currency);
    }

    public Money getDetraction() {
        return new Money(detraction, currency);
    }

    public boolean hasRetention() {
        return hasRetention;
    }

    public boolean hasDetraction() {
        return hasDetraction;
    }

    public boolean isSelfDetraction() {
        return selfDetraction;
    }

    public boolean isAdvance() {
        return advance;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
