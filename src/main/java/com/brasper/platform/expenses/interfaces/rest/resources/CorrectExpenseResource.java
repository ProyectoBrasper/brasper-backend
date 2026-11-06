package com.brasper.platform.expenses.interfaces.rest.resources;

import com.brasper.platform.shared.domain.model.valueobjects.CostCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Replaces all three fields; a null {@code projectId} makes it a general expense. */
public record CorrectExpenseResource(
        Long projectId,
        @NotNull CostCategory category,
        @Size(max = 500) String description) {
}
