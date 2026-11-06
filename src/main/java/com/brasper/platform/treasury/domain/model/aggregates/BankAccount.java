package com.brasper.platform.treasury.domain.model.aggregates;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.treasury.domain.model.commands.OpenBankAccountCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountNumber;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountType;
import com.brasper.platform.treasury.domain.model.valueobjects.Bank;
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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "bank_accounts", uniqueConstraints = @UniqueConstraint(
        name = BankAccount.ACCOUNT_NUMBER_UNIQUE_CONSTRAINT, columnNames = "account_number"))
public class BankAccount {

    public static final String ACCOUNT_NUMBER_UNIQUE_CONSTRAINT = "uk_bank_accounts_account_number";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private Bank bank;

    @Column(name = "account_number", nullable = false, length = 50, updatable = false)
    private AccountNumber accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private AccountType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3, updatable = false)
    private Currency currency;

    @Column(name = "opening_balance", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal openingBalance;

    @Column(name = "opened_on", nullable = false, updatable = false)
    private LocalDate openedOn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BankAccount() {
    }

    public BankAccount(OpenBankAccountCommand command) {
        this.bank = Objects.requireNonNull(command.bank(), "bank");
        this.accountNumber = Objects.requireNonNull(command.accountNumber(), "accountNumber");
        this.type = Objects.requireNonNull(command.type(), "type");
        this.currency = Objects.requireNonNull(command.currency(), "currency");
        Money balance = Objects.requireNonNull(command.openingBalance(), "openingBalance");
        if (balance.currency() != currency) {
            throw new IllegalArgumentException("Opening balance currency must match account currency");
        }
        if (balance.amount().precision() > 14) {
            throw new IllegalArgumentException("Opening balance exceeds numeric(14,2)");
        }
        this.openingBalance = balance.amount();
        this.openedOn = Objects.requireNonNull(command.openedOn(), "openedOn");
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

    public Bank getBank() {
        return bank;
    }

    public AccountNumber getAccountNumber() {
        return accountNumber;
    }

    public AccountType getType() {
        return type;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Money getOpeningBalance() {
        return new Money(openingBalance, currency);
    }

    public LocalDate getOpenedOn() {
        return openedOn;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
