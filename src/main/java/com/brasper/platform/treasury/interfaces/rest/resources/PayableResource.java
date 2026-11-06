package com.brasper.platform.treasury.interfaces.rest.resources;

import java.math.BigDecimal;

public record PayableResource(Long id, String concept, String type, BigDecimal totalAmount, String currency,
                              BigDecimal paidAmount, BigDecimal balance, Long supplierId, Long projectId,
                              Long expenseId, int year, int month, int week, String status) {
}
