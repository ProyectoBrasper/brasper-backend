package com.brasper.platform.accounting.domain.model.aggregates;

import com.brasper.platform.accounting.domain.model.commands.CreateLedgerAccountCommand;
import com.brasper.platform.accounting.domain.model.valueobjects.AccountCode;
import com.brasper.platform.accounting.domain.model.valueobjects.LedgerAccountType;
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
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "ledger_accounts", uniqueConstraints = @UniqueConstraint(
        name = LedgerAccount.CODE_UNIQUE_CONSTRAINT, columnNames = "code"))
public class LedgerAccount {
    public static final String CODE_UNIQUE_CONSTRAINT = "uk_ledger_accounts_code";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 10, updatable = false) private AccountCode code;
    @Column(nullable = false, length = 200) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private LedgerAccountType type;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected LedgerAccount() { }
    public LedgerAccount(CreateLedgerAccountCommand command) {
        if (command.name() == null || command.name().isBlank() || command.name().strip().length() > 200) {
            throw new IllegalArgumentException("Name is required and must not exceed 200 characters");
        }
        code = Objects.requireNonNull(command.code(), "code");
        name = command.name().strip();
        type = Objects.requireNonNull(command.type(), "type");
        active = true;
    }
    public void deactivate() { active = false; }
    public Long getId() { return id; }
    public AccountCode getCode() { return code; }
    public String getName() { return name; }
    public LedgerAccountType getType() { return type; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    @PrePersist
    void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
