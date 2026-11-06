package com.brasper.platform.staff.interfaces.rest.resources;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.staff.domain.model.valueobjects.FixedCostType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record CreateFixedCostResource(@NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 500) String concept, @NotNull FixedCostType type,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal monthlyAmount,
        @NotNull Currency currency, Long employeeId, List<@NotNull @Min(1) @Max(5) Integer> expectedWeeks) {
    public CreateFixedCostResource { expectedWeeks = expectedWeeks == null ? List.of() : expectedWeeks; }
}
