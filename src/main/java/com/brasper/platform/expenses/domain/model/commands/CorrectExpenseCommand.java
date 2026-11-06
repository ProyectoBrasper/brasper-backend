package com.brasper.platform.expenses.domain.model.commands;

import com.brasper.platform.shared.domain.model.valueobjects.CostCategory;

/** Replaces project, category and description; a null project makes it a general expense. */
public record CorrectExpenseCommand(Long expenseId, Long projectId, CostCategory category, String description) {
}
