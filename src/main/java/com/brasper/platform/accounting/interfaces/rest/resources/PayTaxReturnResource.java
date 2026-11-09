package com.brasper.platform.accounting.interfaces.rest.resources;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PayTaxReturnResource(@NotNull LocalDate paidOn, @NotNull Long bankAccountId) { }
