package com.brasper.platform.accounting.domain.model.commands;
import com.brasper.platform.shared.domain.model.valueobjects.Period;

public record RunMonthlyDepreciationCommand(Period period) { }
