package com.brasper.platform.staff.interfaces.rest.resources;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.staff.domain.model.valueobjects.FixedCostType;
import java.math.BigDecimal;
import java.util.List;

public record FixedCostResource(Long id, String code, String concept, FixedCostType type,
        BigDecimal monthlyAmount, Currency currency, Long employeeId, List<Integer> expectedWeeks, boolean active) {}
