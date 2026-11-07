package com.brasper.platform.billing.domain.events;

import java.time.LocalDate;

public record InvoiceVoidedEvent(Long invoiceId, LocalDate voidedOn) { }
