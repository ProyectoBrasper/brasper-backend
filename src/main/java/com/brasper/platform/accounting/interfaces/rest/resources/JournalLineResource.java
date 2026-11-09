package com.brasper.platform.accounting.interfaces.rest.resources;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record JournalLineResource(@NotBlank @Pattern(regexp = "[1-9][0-9]{1,9}") String accountCode,
        @NotNull @Valid MoneyResource debit, @NotNull @Valid MoneyResource credit, Long projectId) { }
