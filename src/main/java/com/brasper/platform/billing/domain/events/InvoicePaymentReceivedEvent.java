package com.brasper.platform.billing.domain.events;

import com.brasper.platform.billing.domain.model.valueobjects.InvoicePaymentType;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import java.time.LocalDate;

/** {@code bankAccountId} is null for a retention certificate, which moves no money. */
public record InvoicePaymentReceivedEvent(Long paymentId, Long invoiceId, LocalDate receivedOn, Money amount,
        InvoicePaymentType type, Long bankAccountId) { }
