package com.brasper.platform.treasury.domain.model.commands;

import com.brasper.platform.shared.domain.model.valueobjects.Money;
import java.time.LocalDate;

public record TransferFundsCommand(Long fromAccountId, Long toAccountId, LocalDate transferredOn,
                                   Money amount, String concept) {
}
