package com.brasper.platform.projects.domain.model.commands;

import com.brasper.platform.shared.domain.model.valueobjects.Percentage;
import java.math.BigDecimal;

/** The budget keeps the project's currency, so only the amount is given. Code and client cannot change. */
public record UpdateProjectCommand(Long projectId, String name, String description, BigDecimal budget,
        Percentage participation, Percentage salesCommissionRate) {
}
