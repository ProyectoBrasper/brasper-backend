package com.brasper.platform.projects.domain.model.commands;

import com.brasper.platform.shared.domain.model.valueobjects.Percentage;
import com.brasper.platform.shared.domain.model.valueobjects.Period;

/** {@code percentage} is the progress earned in that month, not the running total. */
public record RegisterProjectProgressCommand(Long projectId, Period period, Percentage percentage) {
}
