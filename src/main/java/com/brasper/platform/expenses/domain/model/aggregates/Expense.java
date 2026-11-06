package com.brasper.platform.expenses.domain.model.aggregates;

import com.brasper.platform.expenses.domain.model.commands.CorrectExpenseCommand;
import com.brasper.platform.expenses.domain.model.commands.RegisterExpenseCommand;
import com.brasper.platform.expenses.domain.model.valueobjects.PaymentMethod;
import com.brasper.platform.expenses.domain.model.valueobjects.Voucher;
import com.brasper.platform.expenses.domain.model.valueobjects.VoucherType;
import com.brasper.platform.shared.domain.model.valueobjects.CostCategory;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.PaymentStatus;
import com.brasper.platform.shared.domain.model.valueobjects.TaxBreakdown;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A cost the company paid or owes. Only an invoice gives IGV credit: any other voucher is booked without IGV.
 * An expense is never deleted, it is annulled.
 */
@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spent_on", nullable = false, updatable = false)
    private LocalDate spentOn;

    /** Other contexts' aggregates are referenced by id only. Null project = general expense. */
    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "supplier_id", updatable = false)
    private Long supplierId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CostCategory category;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "voucher_type", nullable = false, length = 20, updatable = false)
    private VoucherType voucherType;

    @Column(name = "voucher_number", length = 30, updatable = false)
    private String voucherNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3, updatable = false)
    private Currency currency;

    @Column(name = "taxable_base", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal taxableBase;

    @Column(nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal igv;

    @Column(nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 25, updatable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "bank_account_id", updatable = false)
    private Long bankAccountId;

    @Column(name = "partner_id", updatable = false)
    private Long partnerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(nullable = false)
    private boolean annulled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Expense() {
    }

    /** Taxes follow the voucher: an invoice splits out IGV with the rates in force, anything else has none. */
    public Expense(RegisterExpenseCommand command, TaxRates rates) {
        Objects.requireNonNull(rates, "rates");
        Money amount = Objects.requireNonNull(command.amount(), "amount");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        Voucher voucher = Objects.requireNonNull(command.voucher(), "voucher");
        TaxBreakdown amounts = !voucher.allowsTaxCredit() ? TaxBreakdown.withoutIgv(amount)
                : command.amountIncludesIgv() ? TaxBreakdown.fromTotal(amount, rates.igv())
                : TaxBreakdown.fromTaxableBase(amount, rates.igv());
        this.paymentMethod = Objects.requireNonNull(command.paymentMethod(), "paymentMethod");
        requirePaymentTarget(command);
        this.spentOn = Objects.requireNonNull(command.spentOn(), "spentOn");
        this.projectId = command.projectId();
        this.supplierId = command.supplierId();
        this.category = Objects.requireNonNull(command.category(), "category");
        this.description = blankToNull(command.description());
        this.voucherType = voucher.type();
        this.voucherNumber = voucher.number();
        this.currency = amount.currency();
        this.taxableBase = amounts.taxableBase().amount();
        this.igv = amounts.igv().amount();
        this.total = amounts.total().amount();
        this.bankAccountId = command.bankAccountId();
        this.partnerId = command.partnerId();
        this.paymentStatus = command.paid() ? PaymentStatus.PAID : PaymentStatus.PENDING;
    }

    /** Bank and petty cash payments name the account; a partner contribution names the partner; never both. */
    private void requirePaymentTarget(RegisterExpenseCommand command) {
        boolean partner = paymentMethod == PaymentMethod.PARTNER_CONTRIBUTION;
        if (partner && (command.partnerId() == null || command.bankAccountId() != null)) {
            throw new IllegalArgumentException("A partner contribution needs a partner and no bank account");
        }
        if (!partner && (command.bankAccountId() == null || command.partnerId() != null)) {
            throw new IllegalArgumentException("%s needs a bank account and no partner".formatted(paymentMethod));
        }
    }

    /** Reclassifies the expense; amounts, voucher and payment cannot change, annul and register again instead. */
    public void correct(CorrectExpenseCommand command) {
        requireActive();
        this.category = Objects.requireNonNull(command.category(), "category");
        this.projectId = command.projectId();
        this.description = blankToNull(command.description());
    }

    public void annul() {
        requireActive();
        this.annulled = true;
    }

    /** Treasury settled the payable. */
    public void markAsPaid() {
        requireActive();
        this.paymentStatus = PaymentStatus.PAID;
    }

    private void requireActive() {
        if (annulled) {
            throw new IllegalStateException("The expense is annulled");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    /** A project's expense counts toward its direct cost; no project means a general expense. */
    public boolean isDirectCost() {
        return projectId != null;
    }

    public boolean generatesTaxCredit() {
        return getVoucher().allowsTaxCredit();
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

    public LocalDate getSpentOn() {
        return spentOn;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public CostCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public Voucher getVoucher() {
        return new Voucher(voucherType, voucherNumber);
    }

    public TaxBreakdown getAmounts() {
        return new TaxBreakdown(new Money(taxableBase, currency), new Money(igv, currency),
                new Money(total, currency));
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public Long getBankAccountId() {
        return bankAccountId;
    }

    public Long getPartnerId() {
        return partnerId;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public boolean isAnnulled() {
        return annulled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
