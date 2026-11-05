package com.brasper.platform.billing.domain.model.commands;

import com.brasper.platform.billing.domain.model.valueobjects.InvoiceNumber;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import java.time.LocalDate;

/**
 * The client is not here: it is always the project's, and the service passes it to the aggregate.
 * {@code taxableBase} is the amount before IGV. {@code selfDetraction} means the company deposits the detraction
 * itself, so it only makes sense with {@code hasDetraction}.
 */
public record IssueInvoiceCommand(InvoiceNumber number, LocalDate issuedOn, String purchaseOrder, Long projectId,
        Money taxableBase, boolean hasRetention, boolean hasDetraction, boolean selfDetraction,
        boolean advance, String notes) {
}
