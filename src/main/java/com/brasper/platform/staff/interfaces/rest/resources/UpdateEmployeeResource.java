package com.brasper.platform.staff.interfaces.rest.resources;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateEmployeeResource(
        @Size(max = 100) String position,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal monthlySalary,
        @NotNull Currency currency) {
}
