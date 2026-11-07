package com.brasper.platform.accounting.interfaces.rest.resources;
import com.brasper.platform.accounting.domain.model.valueobjects.LedgerAccountType;
import jakarta.validation.constraints.*;

public record CreateLedgerAccountResource(@NotBlank @Pattern(regexp = "[1-9][0-9]{1,9}") String code,
        @NotBlank @Size(max = 200) String name, @NotNull LedgerAccountType type) { }
