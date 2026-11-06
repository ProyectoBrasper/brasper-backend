package com.brasper.platform.treasury.domain.model.entities;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.treasury.domain.model.aggregates.Partner;
import com.brasper.platform.treasury.domain.model.commands.RegisterPartnerContributionCommand;
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
@Table(name = "partner_contributions")
public class PartnerContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false, updatable = false)
    private Partner partner;

    @Column(name = "contributed_on", nullable = false, updatable = false)
    private LocalDate contributedOn;

    @Column(nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3, updatable = false)
    private Currency currency;

    @Column(nullable = false, updatable = false)
    private boolean reimbursable;

    @Column(name = "bank_account_id", updatable = false)
    private Long bankAccountId;

    @Column(name = "expense_id", updatable = false)
    private Long expenseId;

    @Column(length = 500, updatable = false)
    private String concept;

    protected PartnerContribution() {
    }

    public PartnerContribution(Partner partner, RegisterPartnerContributionCommand command) {
        this.partner = Objects.requireNonNull(partner, "partner");
        this.contributedOn = Objects.requireNonNull(command.contributedOn(), "contributedOn");
        Money money = Objects.requireNonNull(command.amount(), "amount");
        if (!money.isPositive() || money.amount().precision() > 14) {
            throw new IllegalArgumentException("Contribution must be positive and fit numeric(14,2)");
        }
        this.amount = money.amount();
        this.currency = money.currency();
        this.reimbursable = command.reimbursable();
        this.bankAccountId = command.bankAccountId();
        this.expenseId = command.expenseId();
        this.concept = command.concept() == null || command.concept().isBlank() ? null : command.concept().strip();
        if (concept != null && concept.length() > 500) {
            throw new IllegalArgumentException("Concept must not exceed 500 characters");
        }
    }

    public Long getId() {
        return id;
    }

    public LocalDate getContributedOn() {
        return contributedOn;
    }

    public Money getAmount() {
        return new Money(amount, currency);
    }

    public boolean isReimbursable() {
        return reimbursable;
    }

    public Long getBankAccountId() {
        return bankAccountId;
    }

    public Long getExpenseId() {
        return expenseId;
    }

    public String getConcept() {
        return concept;
    }
}
