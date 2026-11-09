package com.brasper.platform.accounting.domain.model.commands;

import java.time.LocalDate;

public record PayTaxReturnCommand(Long taxReturnId, LocalDate paidOn, Long bankAccountId) { }
