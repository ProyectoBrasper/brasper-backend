package com.brasper.platform.billing.domain.model.commands;

import com.brasper.platform.billing.domain.model.valueobjects.InvoicePaymentType;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import java.time.LocalDate;

/** {@code bankAccountId} is not needed for a retention certificate, where no money is deposited. */
public record RegisterInvoicePaymentCommand(Long invoiceId, LocalDate receivedOn, Money amount,
        InvoicePaymentType type, Long bankAccountId, String reference) {
}
