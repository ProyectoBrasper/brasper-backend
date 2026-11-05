package com.brasper.platform.billing.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Every amount is in {@code currency}. {@code netAmount} is what the client deposits to the company. */
public record InvoiceResource(Long id, String number, LocalDate issuedOn, String purchaseOrder, Long projectId,
        Long clientId, String currency, BigDecimal taxableBase, BigDecimal igv, BigDecimal total,
        BigDecimal retention, BigDecimal detraction, boolean selfDetraction, boolean advance,
        BigDecimal netAmount, BigDecimal collectedAmount, BigDecimal outstandingBalance, String status,
        String notes) {
}
