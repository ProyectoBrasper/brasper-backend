package com.brasper.platform.accounting.domain.model.entities;

import com.brasper.platform.accounting.domain.model.aggregates.JournalEntry;
import com.brasper.platform.accounting.domain.model.commands.JournalLineData;
import com.brasper.platform.accounting.domain.model.valueobjects.AccountCode;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "journal_lines")
public class JournalLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_entry_id", nullable = false, updatable = false) private JournalEntry journalEntry;
    @Column(name = "account_code", nullable = false, length = 10, updatable = false) private AccountCode accountCode;
    @Column(nullable = false, precision = 14, scale = 2, updatable = false) private BigDecimal debit;
    @Column(nullable = false, precision = 14, scale = 2, updatable = false) private BigDecimal credit;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 3, updatable = false) private Currency currency;
    @Column(name = "project_id", updatable = false) private Long projectId;

    protected JournalLine() { }
    public JournalLine(JournalEntry entry, JournalLineData data) {
        journalEntry = Objects.requireNonNull(entry, "entry");
        if (data == null || data.accountCode() == null || data.debit() == null || data.credit() == null) {
            throw new IllegalArgumentException("Account, debit and credit are required");
        }
        Money d = data.debit(), c = data.credit();
        if (d.currency() != c.currency() || d.amount().signum() < 0 || c.amount().signum() < 0
                || d.isPositive() == c.isPositive() || d.amount().precision() > 14 || c.amount().precision() > 14) {
            throw new IllegalArgumentException("Exactly one amount must be positive; amounts must fit numeric(14,2) and share currency");
        }
        accountCode = data.accountCode(); debit = d.amount(); credit = c.amount();
        currency = d.currency(); projectId = data.projectId();
    }
    public Long getId() { return id; }
    public JournalEntry getJournalEntry() { return journalEntry; }
    public AccountCode getAccountCode() { return accountCode; }
    public Money getDebit() { return new Money(debit, currency); }
    public Money getCredit() { return new Money(credit, currency); }
    public Long getProjectId() { return projectId; }
}
