package com.brasper.platform.treasury.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PayInstallmentResource(@NotNull LocalDate paidOn, @NotNull Long bankAccountId) {
}
