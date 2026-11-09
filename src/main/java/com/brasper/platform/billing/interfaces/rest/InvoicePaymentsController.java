package com.brasper.platform.billing.interfaces.rest;

import com.brasper.platform.billing.application.commandservices.InvoiceCommandFailure;
import com.brasper.platform.billing.application.commandservices.InvoiceCommandService;
import com.brasper.platform.billing.application.queryservices.InvoiceQueryService;
import com.brasper.platform.billing.domain.model.commands.RegisterInvoicePaymentCommand;
import com.brasper.platform.billing.interfaces.rest.resources.RegisterInvoicePaymentResource;
import com.brasper.platform.billing.interfaces.rest.transform.InvoiceFailureResponseAssembler;
import com.brasper.platform.billing.interfaces.rest.transform.InvoicePaymentResourceFromEntityAssembler;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Any signed-in user reads; only admin and accounting register payments. */
@RestController
@RequestMapping("/api/v1/invoices/{invoiceId}/payments")
@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTING','MANAGEMENT')")
public class InvoicePaymentsController {

    private final InvoiceCommandService invoiceCommands;
    private final InvoiceQueryService invoiceQueries;
    private final MessageSource messages;

    public InvoicePaymentsController(InvoiceCommandService invoiceCommands, InvoiceQueryService invoiceQueries,
            MessageSource messages) {
        this.invoiceCommands = invoiceCommands;
        this.invoiceQueries = invoiceQueries;
        this.messages = messages;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTING')")
    public ResponseEntity<?> register(@PathVariable Long invoiceId,
            @Valid @RequestBody RegisterInvoicePaymentResource resource) {
        // A deposit without a bank account throws IllegalArgumentException, mapped to 400 by the global handler.
        var command = new RegisterInvoicePaymentCommand(invoiceId, resource.receivedOn(),
                new Money(resource.amount(), resource.currency()), resource.type(), resource.bankAccountId(),
                resource.reference());
        return invoiceCommands.handle(command).fold(
                payment -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(InvoicePaymentResourceFromEntityAssembler.toResourceFromEntity(payment)),
                failure -> InvoiceFailureResponseAssembler.toResponse(failure, messages));
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable Long invoiceId) {
        return invoiceQueries.findPayments(invoiceId)
                .<ResponseEntity<?>>map(payments -> ResponseEntity.ok(payments.stream()
                        .map(InvoicePaymentResourceFromEntityAssembler::toResourceFromEntity).toList()))
                .orElseGet(() -> InvoiceFailureResponseAssembler.toResponse(
                        InvoiceCommandFailure.INVOICE_NOT_FOUND, messages));
    }
}
