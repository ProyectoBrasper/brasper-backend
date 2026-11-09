package com.brasper.platform.billing.domain.model.aggregates;

import com.brasper.platform.billing.domain.model.commands.ScheduleCollectionCommand;
import com.brasper.platform.billing.domain.model.valueobjects.ScheduleStatus;
import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
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
import java.util.Objects;

/**
 * A collection the cash flow expects in a given week, before the invoice exists. It is realized when the real
 * invoice is issued, and can be moved to another week or cancelled until then.
 */
@Entity
@Table(name = "scheduled_collections")
public class ScheduledCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Other contexts' aggregates are referenced by id only. */
    @Column(name = "project_id", nullable = false, updatable = false)
    private Long projectId;

    @Column(name = "expected_amount", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal expectedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3, updatable = false)
    private Currency currency;

    @Column(name = "expected_year", nullable = false)
    private int expectedYear;

    @Column(name = "expected_month", nullable = false)
    private int expectedMonth;

    @Column(name = "expected_week", nullable = false)
    private int expectedWeek;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleStatus status = ScheduleStatus.SCHEDULED;

    /** The invoice that realized this collection, from the same context. */
    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ScheduledCollection() {
    }

    public ScheduledCollection(ScheduleCollectionCommand command) {
        Money amount = Objects.requireNonNull(command.expectedAmount(), "expectedAmount");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Expected amount must be greater than zero");
        }
        this.projectId = Objects.requireNonNull(command.projectId(), "projectId");
        this.expectedAmount = amount.amount();
        this.currency = amount.currency();
        place(Objects.requireNonNull(command.expectedWeek(), "expectedWeek"));
        this.notes = command.notes() == null || command.notes().isBlank() ? null : command.notes().strip();
    }

    /** Moves an open collection to another week. */
    public void reschedule(CashFlowWeek week) {
        requireOpen("rescheduled");
        place(Objects.requireNonNull(week, "week"));
        status = ScheduleStatus.RESCHEDULED;
    }

    /** The real invoice was issued, so the projection is no longer needed. */
    public void markAsRealized(Long invoiceId) {
        requireOpen("realized");
        this.invoiceId = Objects.requireNonNull(invoiceId, "invoiceId");
        status = ScheduleStatus.REALIZED;
    }

    public void cancel() {
        requireOpen("cancelled");
        status = ScheduleStatus.CANCELLED;
    }

    private void requireOpen(String action) {
        if (!status.isOpen()) {
            throw new IllegalStateException("A %s collection cannot be %s".formatted(status, action));
        }
    }

    private void place(CashFlowWeek week) {
        this.expectedYear = week.year();
        this.expectedMonth = week.month();
        this.expectedWeek = week.number();
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

    public Long getProjectId() {
        return projectId;
    }

    public Money getExpectedAmount() {
        return new Money(expectedAmount, currency);
    }

    public CashFlowWeek getExpectedWeek() {
        return new CashFlowWeek(expectedYear, expectedMonth, expectedWeek);
    }

    public ScheduleStatus getStatus() {
        return status;
    }

    public Long getInvoiceId() {
        return invoiceId;
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
