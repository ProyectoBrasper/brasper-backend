package com.brasper.platform.accounting.domain.model.commands;

import java.time.LocalDate;

public record ReverseJournalEntryCommand(Long journalEntryId, LocalDate reversedOn) { }
