package com.brasper.platform.treasury.interfaces.rest.resources;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.treasury.domain.model.valueobjects.LoanType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterLoanResource(
        @NotNull LoanType type,
        @NotBlank @Size(max = 200) String counterparty,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal principal,
        @NotNull Currency currency,
        @NotNull @PositiveOrZero BigDecimal interestRate,
        @NotNull LocalDate disbursedOn,
        @NotNull Long bankAccountId) {
}
