package com.brasper.platform.treasury.domain.model.queries;

import com.brasper.platform.shared.domain.model.valueobjects.Period;

public record GetCashMovementsByAccountAndPeriodQuery(Long bankAccountId, Period period) {
}
