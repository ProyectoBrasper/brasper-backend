package com.brasper.platform.treasury.interfaces.rest.resources;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountType;
import com.brasper.platform.treasury.domain.model.valueobjects.Bank;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record OpenBankAccountResource(
        @NotNull Bank bank,
        @NotBlank @Size(max = 50) String accountNumber,
        @NotNull AccountType type,
        @NotNull Currency currency,
        @NotNull @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal openingBalance,
        @NotNull LocalDate openedOn) {
}
