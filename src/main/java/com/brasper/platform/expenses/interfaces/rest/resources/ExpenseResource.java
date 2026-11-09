package com.brasper.platform.expenses.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResource(
        Long id,
        LocalDate spentOn,
        Long projectId,
        Long supplierId,
        String category,
        String description,
        String voucherType,
        String voucherNumber,
        BigDecimal taxableBase,
        BigDecimal igv,
        BigDecimal total,
        String currency,
        String paymentMethod,
        Long bankAccountId,
        Long partnerId,
        String paymentStatus,
        boolean annulled) {
}
