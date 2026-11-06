package com.brasper.platform.accounting.domain.model.commands;

import com.brasper.platform.shared.domain.model.valueobjects.Money;
import java.time.LocalDate;

public record RegisterFixedAssetCommand(String description, LocalDate acquiredOn, Money taxableBase,
        int usefulLifeMonths, Long expenseId) { }
