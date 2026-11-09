package com.brasper.platform.accounting.domain.model.aggregates;

import com.brasper.platform.accounting.domain.model.commands.*;
import com.brasper.platform.accounting.domain.model.entities.JournalLine;
import com.brasper.platform.accounting.domain.model.valueobjects.EntrySource;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "journal_entries")
public class JournalEntry {
    public static final String SOURCE_UNIQUE_CONSTRAINT = "uk_journal_entries_source";
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "entry_date", nullable = false, updatable = false) private LocalDate entryDate;
    @Column(name = "period_year", nullable = false, updatable = false) private int periodYear;
    @Column(name = "period_month", nullable = false, updatable = false) private int periodMonth;
    @Column(nullable = false, length = 300, updatable = false) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30, updatable = false) private EntrySource source;
    @Column(name = "source_id", updatable = false) private Long sourceId;
    @Column(nullable = false) private boolean reversed;
    @Column(name = "reversal_of_id", updatable = false) private Long reversalOfId;
    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL) @OrderBy("id ASC")
    private List<JournalLine> lines = new ArrayList<>();
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected JournalEntry() { }
    public JournalEntry(RegisterJournalEntryCommand command) {
        if (command == null || command.entryDate() == null || command.source() == null
                || command.description() == null || command.description().isBlank()
                || command.description().strip().length() > 300) {
            throw new IllegalArgumentException("Date, source and description (up to 300 characters) are required");
        }
        if (command.lines() == null || command.lines().size() < 2) throw new IllegalArgumentException("At least two lines are required");
        entryDate = command.entryDate();
        Period period = Period.from(entryDate); periodYear = period.year(); periodMonth = period.month();
        description = command.description().strip(); source = command.source(); sourceId = command.sourceId();
        for (JournalLineData data : command.lines()) lines.add(new JournalLine(this, data));
        if (!totalDebit().equals(totalCredit())) throw new IllegalArgumentException("Entry must balance");
    }
    public Money totalDebit() {
        return lines.stream().map(JournalLine::getDebit).reduce(Money.zero(lines.getFirst().getDebit().currency()), Money::add);
    }
    public Money totalCredit() {
        return lines.stream().map(JournalLine::getCredit).reduce(Money.zero(lines.getFirst().getDebit().currency()), Money::add);
    }
    public JournalEntry reverse(LocalDate reversedOn) {
        if (reversed || source == EntrySource.REVERSAL || id == null) throw new IllegalStateException("Entry cannot be reversed");
        String text = "Reversal of: " + description;
        JournalEntry reversal = new JournalEntry(new RegisterJournalEntryCommand(reversedOn,
                text.substring(0, Math.min(300, text.length())), EntrySource.REVERSAL, id,
                lines.stream().map(l -> new JournalLineData(l.getAccountCode(), l.getCredit(), l.getDebit(), l.getProjectId())).toList()));
        reversal.reversalOfId = id; reversed = true;
        return reversal;
    }
    public Long getId() { return id; }
    public LocalDate getEntryDate() { return entryDate; }
    public Period getPeriod() { return new Period(periodYear, periodMonth); }
    public String getDescription() { return description; }
    public EntrySource getSource() { return source; }
    public Long getSourceId() { return sourceId; }
    public boolean isReversed() { return reversed; }
    public Long getReversalOfId() { return reversalOfId; }
    public List<JournalLine> getLines() { return List.copyOf(lines); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
