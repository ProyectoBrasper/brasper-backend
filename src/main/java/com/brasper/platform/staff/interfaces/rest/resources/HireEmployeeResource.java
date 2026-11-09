package com.brasper.platform.staff.interfaces.rest.resources;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record HireEmployeeResource(
        @NotBlank @Size(max = 200) String fullName,
        @NotNull @Pattern(regexp = "[0-9]{8}") String dni,
        @Size(max = 100) String position,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal monthlySalary,
        @NotNull Currency currency,
        @NotNull LocalDate hiredOn) {
}
