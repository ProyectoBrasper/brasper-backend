package com.brasper.platform.projects.domain.model.commands;

import com.brasper.platform.projects.domain.model.valueobjects.ProjectCode;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Percentage;
import java.time.LocalDate;

/** {@code budget} is the taxable base (without IGV). */
public record CreateProjectCommand(ProjectCode code, String name, String description, Long clientId, Money budget,
        LocalDate quotedOn, Percentage participation, Percentage salesCommissionRate) {
}
