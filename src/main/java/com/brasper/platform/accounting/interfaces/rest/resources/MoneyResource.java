package com.brasper.platform.accounting.interfaces.rest.resources;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record MoneyResource(@NotNull @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @NotNull Currency currency) { }
