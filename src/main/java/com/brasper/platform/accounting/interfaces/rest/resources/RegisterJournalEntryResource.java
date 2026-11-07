package com.brasper.platform.accounting.interfaces.rest.resources;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public record RegisterJournalEntryResource(@NotNull LocalDate entryDate, @NotBlank @Size(max = 300) String description,
        @NotEmpty List<@NotNull @Valid JournalLineResource> lines) { }
