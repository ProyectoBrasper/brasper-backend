package com.brasper.platform.staff.interfaces.rest.resources;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record UpdateFixedCostResource(@NotBlank @Size(max = 500) String concept,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal monthlyAmount,
        List<@NotNull @Min(1) @Max(5) Integer> expectedWeeks) {
    public UpdateFixedCostResource { expectedWeeks = expectedWeeks == null ? List.of() : expectedWeeks; }
}
