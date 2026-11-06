package com.brasper.platform.accounting.interfaces.rest.resources;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterFixedAssetResource(@NotBlank @Size(max = 300) String description,
        @NotNull LocalDate acquiredOn,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal taxableBase,
        @NotNull Currency currency, @Min(1) @Max(600) int usefulLifeMonths, Long expenseId) { }
