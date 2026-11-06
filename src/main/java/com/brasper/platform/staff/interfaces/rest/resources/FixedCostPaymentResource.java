package com.brasper.platform.staff.interfaces.rest.resources;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FixedCostPaymentResource(Long id, Long fixedCostId, LocalDate paidOn, int year, int month,
        BigDecimal amount, Currency currency, Long bankAccountId, String notes) {}
