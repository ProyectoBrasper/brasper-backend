package com.brasper.platform.billing.interfaces.rest.resources;

import java.math.BigDecimal;

public record ScheduledCollectionResource(
        Long id,
        Long projectId,
        BigDecimal expectedAmount,
        String currency,
        int year,
        int month,
        int week,
        String status,
        Long invoiceId,
        String notes) {
}
