package com.brasper.platform.billing.interfaces.rest.resources;

import com.brasper.platform.billing.domain.model.valueobjects.InvoicePaymentType;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * {@code currency} must be the invoice's. {@code bankAccountId} is required for the two deposit types and not
 * needed for a retention certificate.
 */
public record RegisterInvoicePaymentResource(
        @NotNull LocalDate receivedOn,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @NotNull Currency currency,
        @NotNull InvoicePaymentType type,
        Long bankAccountId,
        @Size(max = 100) String reference) {
}
