package com.brasper.platform.staff.domain.model.commands;

import java.time.LocalDate;

public record TerminateEmployeeCommand(Long employeeId, LocalDate terminatedOn) { }
