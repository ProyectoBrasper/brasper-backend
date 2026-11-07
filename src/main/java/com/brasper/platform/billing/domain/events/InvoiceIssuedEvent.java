package com.brasper.platform.billing.domain.events;

import com.brasper.platform.shared.domain.model.valueobjects.Money;
import java.time.LocalDate;

public record InvoiceIssuedEvent(Long invoiceId, Long projectId, LocalDate issuedOn, Money taxableBase, Money igv,
        Money total, boolean advance) { }
