package com.brasper.platform.accounting.domain.model.commands;

import com.brasper.platform.accounting.domain.model.valueobjects.EntrySource;
import java.time.LocalDate;
import java.util.List;

public record RegisterJournalEntryCommand(LocalDate entryDate, String description, EntrySource source,
        Long sourceId, List<JournalLineData> lines) { }
