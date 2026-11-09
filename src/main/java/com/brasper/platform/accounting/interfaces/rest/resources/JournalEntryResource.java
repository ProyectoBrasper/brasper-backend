package com.brasper.platform.accounting.interfaces.rest.resources;
import com.brasper.platform.accounting.domain.model.valueobjects.EntrySource;
import java.time.LocalDate;
import java.util.List;

public record JournalEntryResource(Long id, LocalDate entryDate, int year, int month, String description,
        EntrySource source, Long sourceId, boolean reversed, Long reversalOfId, MoneyResource totalDebit,
        MoneyResource totalCredit, List<JournalLineResource> lines) { }
