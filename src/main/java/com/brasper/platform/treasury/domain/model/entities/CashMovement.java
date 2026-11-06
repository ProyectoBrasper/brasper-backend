package com.brasper.platform.treasury.domain.model.entities;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.treasury.domain.model.aggregates.BankAccount;
import com.brasper.platform.treasury.domain.model.commands.RegisterCashMovementCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementDirection;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementSource;
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

@Entity
@Table(name = "cash_movements")
public class CashMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bank_account_id", nullable = false, updatable = false)
    private BankAccount bankAccount;
    @Column(name = "occurred_on", nullable = false, updatable = false)
    private LocalDate occurredOn;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private MovementDirection direction;
    @Column(nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3, updatable = false)
    private Currency currency;
    @Column(length = 500, updatable = false)
    private String concept;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25, updatable = false)
    private MovementSource source;
    @Column(name = "source_id", updatable = false)
    private Long sourceId;

    protected CashMovement() {
    }

    public CashMovement(BankAccount bankAccount, RegisterCashMovementCommand command) {
        this.bankAccount = Objects.requireNonNull(bankAccount, "bankAccount");
        Money money = Objects.requireNonNull(command.amount(), "amount");
        if (money.currency() != bankAccount.getCurrency() || !money.isPositive() || money.amount().precision() > 14) {
            throw new IllegalArgumentException("Movement must be positive, fit numeric(14,2) and match account currency");
        }
        this.occurredOn = Objects.requireNonNull(command.occurredOn(), "occurredOn");
        this.direction = Objects.requireNonNull(command.direction(), "direction");
        this.amount = money.amount();
        this.currency = money.currency();
        this.concept = command.concept() == null || command.concept().isBlank() ? null : command.concept().strip();
        if (concept != null && concept.length() > 500) {
            throw new IllegalArgumentException("Concept must not exceed 500 characters");
        }
        this.source = Objects.requireNonNull(command.source(), "source");
        this.sourceId = command.sourceId();
    }

    public Long getId() {
        return id;
    }

    public LocalDate getOccurredOn() {
        return occurredOn;
    }

    public MovementDirection getDirection() {
        return direction;
    }

    public Money getAmount() {
        return new Money(amount, currency);
    }

    public String getConcept() {
        return concept;
    }

    public MovementSource getSource() {
        return source;
    }

    public Long getSourceId() {
        return sourceId;
    }
}
