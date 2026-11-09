package com.brasper.platform.billing.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;

/** The amount is in the invoice's currency. */
public record InvoicePaymentResource(Long id, LocalDate receivedOn, BigDecimal amount, String type,
        Long bankAccountId, String reference) {
}
