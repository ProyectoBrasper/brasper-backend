package com.brasper.platform.billing.interfaces.rest;

import com.brasper.platform.billing.application.commandservices.InvoiceCommandFailure;
import com.brasper.platform.billing.application.commandservices.InvoiceCommandService;
import com.brasper.platform.billing.application.queryservices.InvoiceQueryService;
import com.brasper.platform.billing.domain.model.commands.IssueInvoiceCommand;
import com.brasper.platform.billing.domain.model.commands.VoidInvoiceCommand;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceNumber;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceStatus;
import com.brasper.platform.billing.interfaces.rest.resources.IssueInvoiceResource;
import com.brasper.platform.billing.interfaces.rest.transform.InvoiceFailureResponseAssembler;
import com.brasper.platform.billing.interfaces.rest.transform.InvoiceResourceFromEntityAssembler;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Any signed-in user reads; only admin and accounting issue or void invoices. */
@RestController
@RequestMapping("/api/v1/invoices")
@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTING','MANAGEMENT')")
public class InvoicesController {

    private static final String CAN_WRITE = "hasAnyRole('ADMIN','ACCOUNTING')";

    private final InvoiceCommandService invoiceCommands;
    private final InvoiceQueryService invoiceQueries;
    private final MessageSource messages;

    public InvoicesController(InvoiceCommandService invoiceCommands, InvoiceQueryService invoiceQueries,
            MessageSource messages) {
        this.invoiceCommands = invoiceCommands;
        this.invoiceQueries = invoiceQueries;
        this.messages = messages;
    }

    @PostMapping
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> issue(@Valid @RequestBody IssueInvoiceResource resource) {
        // A malformed series throws IllegalArgumentException, mapped to 400 by the global handler.
        var command = new IssueInvoiceCommand(new InvoiceNumber(resource.series(), resource.correlative()),
                resource.issuedOn(), resource.purchaseOrder(), resource.projectId(),
                new Money(resource.taxableBase(), resource.currency()), resource.hasRetention(),
                resource.hasDetraction(), resource.selfDetraction(), resource.advance(), resource.notes());
        return invoiceCommands.handle(command).fold(
                invoice -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(InvoiceResourceFromEntityAssembler.toResourceFromEntity(invoice)),
                failure -> InvoiceFailureResponseAssembler.toResponse(failure, messages));
    }

    @PostMapping("/{id}/void")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> voidInvoice(@PathVariable Long id) {
        return invoiceCommands.handle(new VoidInvoiceCommand(id)).fold(
                invoice -> ResponseEntity.ok(InvoiceResourceFromEntityAssembler.toResourceFromEntity(invoice)),
                failure -> InvoiceFailureResponseAssembler.toResponse(failure, messages));
    }

    /** {@code year} and {@code month} filter by issue month and must come together. */
    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) Long projectId, @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        if ((year == null) != (month == null)) {
            throw new IllegalArgumentException("year and month must be given together");
        }
        Period issuedIn = year == null ? null : new Period(year, month);
        return ResponseEntity.ok(invoiceQueries.findAll(status, projectId, issuedIn).stream()
                .map(InvoiceResourceFromEntityAssembler::toResourceFromEntity).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return invoiceQueries.findById(id)
                .<ResponseEntity<?>>map(invoice -> ResponseEntity.ok(
                        InvoiceResourceFromEntityAssembler.toResourceFromEntity(invoice)))
                .orElseGet(() -> InvoiceFailureResponseAssembler.toResponse(
                        InvoiceCommandFailure.INVOICE_NOT_FOUND, messages));
    }
}
