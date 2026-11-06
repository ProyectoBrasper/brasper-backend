package com.brasper.platform.staff.domain.model.commands;

import com.brasper.platform.staff.domain.model.valueobjects.Dni;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import java.time.LocalDate;

public record HireEmployeeCommand(String fullName, Dni dni, String position, Money monthlySalary, LocalDate hiredOn) { }
