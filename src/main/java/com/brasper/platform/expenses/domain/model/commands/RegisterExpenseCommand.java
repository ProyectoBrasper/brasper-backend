package com.brasper.platform.expenses.domain.model.commands;

import com.brasper.platform.expenses.domain.model.valueobjects.PaymentMethod;
import com.brasper.platform.expenses.domain.model.valueobjects.Voucher;
import com.brasper.platform.shared.domain.model.valueobjects.CostCategory;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import java.time.LocalDate;

/**
 * {@code projectId} null is a general expense. {@code amountIncludesIgv} only matters for an invoice.
 * {@code paid} false leaves a payable for Treasury.
 */
public record RegisterExpenseCommand(
        LocalDate spentOn,
        Long projectId,
        Long supplierId,
        CostCategory category,
        String description,
        Voucher voucher,
        Money amount,
        boolean amountIncludesIgv,
        PaymentMethod paymentMethod,
        Long bankAccountId,
        Long partnerId,
        boolean paid) {
}
