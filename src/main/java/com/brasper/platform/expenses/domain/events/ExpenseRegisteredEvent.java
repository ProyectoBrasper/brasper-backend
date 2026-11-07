package com.brasper.platform.expenses.domain.events;

import com.brasper.platform.expenses.domain.model.valueobjects.PaymentMethod;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import java.time.LocalDate;

/** {@code projectId} is null for a general expense. {@code bankAccountId} is null for a partner contribution. */
public record ExpenseRegisteredEvent(Long expenseId, LocalDate spentOn, Long projectId, Money taxableBase, Money igv,
        Money total, boolean paid, PaymentMethod paymentMethod, Long bankAccountId, Long partnerId) { }
