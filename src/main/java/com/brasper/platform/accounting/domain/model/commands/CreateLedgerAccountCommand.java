package com.brasper.platform.accounting.domain.model.commands;

import com.brasper.platform.accounting.domain.model.valueobjects.AccountCode;
import com.brasper.platform.accounting.domain.model.valueobjects.LedgerAccountType;

public record CreateLedgerAccountCommand(AccountCode code, String name, LedgerAccountType type) { }
