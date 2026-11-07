package com.brasper.platform.expenses.domain.events;

import com.brasper.platform.shared.domain.model.valueobjects.Money;
import java.time.LocalDate;

/** {@code bankAccountId} is the account the expense was paid from; null for a partner contribution. */
public record ExpenseAnnulledEvent(Long expenseId, LocalDate annulledOn, boolean paid, Long bankAccountId,
        Money total) { }
