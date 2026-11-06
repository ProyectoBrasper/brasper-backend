package com.brasper.platform.treasury.domain.model.commands;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountNumber;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountType;
import com.brasper.platform.treasury.domain.model.valueobjects.Bank;
import java.time.LocalDate;

public record OpenBankAccountCommand(Bank bank, AccountNumber accountNumber, AccountType type,
                                     Currency currency, Money openingBalance, LocalDate openedOn) {
}
