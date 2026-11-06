package com.brasper.platform.staff.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record TerminateEmployeeResource(@NotNull LocalDate terminatedOn) {
}
