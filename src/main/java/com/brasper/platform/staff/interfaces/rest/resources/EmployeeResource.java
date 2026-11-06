package com.brasper.platform.staff.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeResource(Long id, String fullName, String dni, String position, BigDecimal monthlySalary,
        String currency, LocalDate hiredOn, LocalDate terminatedOn, boolean active) {
}
