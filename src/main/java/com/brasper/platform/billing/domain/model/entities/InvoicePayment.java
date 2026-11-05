package com.brasper.platform.billing.domain.model.entities;

import com.brasper.platform.billing.domain.model.aggregates.Invoice;
import com.brasper.platform.billing.domain.model.commands.RegisterInvoicePaymentCommand;
import com.brasper.platform.billing.domain.model.valueobjects.InvoicePaymentType;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** Money (or a retention certificate) received against an invoice. Created only through {@code Invoice.registerPayment}. */
@Entity
@Table(name = "invoice_payments")
public class InvoicePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false, updatable = false)
    private Invoice invoice;

    @Column(name = "received_on", nullable = false, updatable = false)
    private LocalDate receivedOn;

    @Column(nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25, updatable = false)
    private InvoicePaymentType type;

    /** Treasury's bank account, by id only. */
    @Column(name = "bank_account_id", updatable = false)
    private Long bankAccountId;

    @Column(length = 100, updatable = false)
    private String reference;

    protected InvoicePayment() {
    }

    public InvoicePayment(Invoice invoice, RegisterInvoicePaymentCommand command) {
        this.invoice = Objects.requireNonNull(invoice, "invoice");
        this.receivedOn = Objects.requireNonNull(command.receivedOn(), "receivedOn");
        this.amount = Objects.requireNonNull(command.amount(), "amount").amount();
        this.type = Objects.requireNonNull(command.type(), "type");
        this.bankAccountId = command.bankAccountId();
        this.reference = command.reference() == null || command.reference().isBlank() ? null
                : command.reference().strip();
    }

    public Long getId() {
        return id;
    }

    public LocalDate getReceivedOn() {
        return receivedOn;
    }

    /** In the invoice's currency. */
    public Money getAmount() {
        return new Money(amount, invoice.getTotal().currency());
    }

    public InvoicePaymentType getType() {
        return type;
    }

    public Long getBankAccountId() {
        return bankAccountId;
    }

    public String getReference() {
        return reference;
    }
}
