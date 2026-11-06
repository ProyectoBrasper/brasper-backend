package com.brasper.platform.expenses.interfaces.rest.resources;

import com.brasper.platform.expenses.domain.model.valueobjects.PaymentMethod;
import com.brasper.platform.expenses.domain.model.valueobjects.VoucherType;
import com.brasper.platform.shared.domain.model.valueobjects.CostCategory;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * {@code projectId} null is a general expense. {@code amountIncludesIgv} (default false) only matters for an
 * invoice. {@code paid} is required: false leaves a payable for Treasury. Bank account and petty cash payments
 * need {@code bankAccountId}; a partner contribution needs {@code partnerId}.
 */
public record RegisterExpenseResource(
        @NotNull LocalDate spentOn,
        Long projectId,
        Long supplierId,
        @NotNull CostCategory category,
        @Size(max = 500) String description,
        @NotNull VoucherType voucherType,
        @Size(max = 30) String voucherNumber,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
        Boolean amountIncludesIgv,
        @NotNull Currency currency,
        @NotNull PaymentMethod paymentMethod,
        Long bankAccountId,
        Long partnerId,
        @NotNull Boolean paid) {

    public RegisterExpenseResource {
        amountIncludesIgv = amountIncludesIgv != null && amountIncludesIgv;
    }
}
