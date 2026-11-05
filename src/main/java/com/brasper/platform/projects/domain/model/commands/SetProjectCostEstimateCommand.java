package com.brasper.platform.projects.domain.model.commands;

import com.brasper.platform.shared.domain.model.valueobjects.CostCategory;
import com.brasper.platform.shared.domain.model.valueobjects.Money;

/** Sets (or replaces) the estimate of one category; the amount must be in the project's currency. */
public record SetProjectCostEstimateCommand(Long projectId, CostCategory category, Money estimatedAmount) {
}
