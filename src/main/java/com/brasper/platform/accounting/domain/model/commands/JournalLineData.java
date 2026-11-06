package com.brasper.platform.accounting.domain.model.commands;

import com.brasper.platform.accounting.domain.model.valueobjects.AccountCode;
import com.brasper.platform.shared.domain.model.valueobjects.Money;

public record JournalLineData(AccountCode accountCode, Money debit, Money credit, Long projectId) { }
