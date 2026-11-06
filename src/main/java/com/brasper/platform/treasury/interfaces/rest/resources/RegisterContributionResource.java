package com.brasper.platform.treasury.interfaces.rest.resources;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** {@code bankAccountId} is optional: the contribution can be an expense the partner paid out of pocket. */
public record RegisterContributionResource(
        @NotNull LocalDate contributedOn,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @NotNull Currency currency,
        boolean reimbursable,
        Long bankAccountId,
        @Size(max = 500) String concept) {
}
