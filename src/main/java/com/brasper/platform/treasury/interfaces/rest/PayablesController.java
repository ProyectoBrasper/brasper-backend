package com.brasper.platform.treasury.interfaces.rest;

import com.brasper.platform.shared.application.result.Result;
import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.PaymentStatus;
import com.brasper.platform.treasury.application.commandservices.PayableCommandFailure;
import com.brasper.platform.treasury.application.commandservices.PayableCommandService;
import com.brasper.platform.treasury.application.queryservices.PayableQueryService;
import com.brasper.platform.treasury.domain.model.aggregates.Payable;
import com.brasper.platform.treasury.domain.model.commands.CreatePayableCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterPayablePaymentCommand;
import com.brasper.platform.treasury.domain.model.commands.ReschedulePayableCommand;
import com.brasper.platform.treasury.interfaces.rest.resources.CreatePayableResource;
import com.brasper.platform.treasury.interfaces.rest.resources.RegisterPayablePaymentResource;
import com.brasper.platform.treasury.interfaces.rest.resources.ReschedulePayableResource;
import com.brasper.platform.treasury.interfaces.rest.transform.PayableFailureResponseAssembler;
import com.brasper.platform.treasury.interfaces.rest.transform.PayableResourceAssembler;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payables")
@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTING','MANAGEMENT')")
public class PayablesController {

    private static final String CAN_WRITE = "hasAnyRole('ADMIN','ACCOUNTING')";
    private final PayableCommandService commands;
    private final PayableQueryService queries;
    private final MessageSource messages;

    public PayablesController(PayableCommandService commands, PayableQueryService queries, MessageSource messages) {
        this.commands = commands;
        this.queries = queries;
        this.messages = messages;
    }

    @PostMapping
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> create(@Valid @RequestBody CreatePayableResource resource) {
        var command = new CreatePayableCommand(resource.concept(), resource.type(),
                new Money(resource.totalAmount(), resource.currency()), resource.supplierId(), resource.projectId(),
                resource.expenseId(), new CashFlowWeek(resource.year(), resource.month(), resource.week()));
        return respond(commands.handle(command), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) PaymentStatus status) {
        return ResponseEntity.ok(queries.findAll().stream()
                .filter(payable -> status == null || payable.getStatus() == status)
                .map(PayableResourceAssembler::toResource).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return queries.findById(id)
                .<ResponseEntity<?>>map(payable -> ResponseEntity.ok(PayableResourceAssembler.toResource(payable)))
                .orElseGet(() -> notFound());
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> pay(@PathVariable Long id, @Valid @RequestBody RegisterPayablePaymentResource resource) {
        return queries.findById(id).<ResponseEntity<?>>map(payable -> {
            var command = new RegisterPayablePaymentCommand(id, resource.paidOn(),
                    new Money(resource.amount(), payable.getTotalAmount().currency()), resource.bankAccountId());
            return respond(commands.handle(command), HttpStatus.CREATED);
        }).orElseGet(() -> notFound());
    }

    @PutMapping("/{id}/schedule")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> reschedule(@PathVariable Long id, @Valid @RequestBody ReschedulePayableResource resource) {
        var command = new ReschedulePayableCommand(id,
                new CashFlowWeek(resource.year(), resource.month(), resource.week()));
        return respond(commands.handle(command), HttpStatus.OK);
    }

    private ResponseEntity<?> respond(Result<Payable, PayableCommandFailure> result, HttpStatus status) {
        return result.<ResponseEntity<?>>fold(
                payable -> ResponseEntity.status(status).body(PayableResourceAssembler.toResource(payable)),
                failure -> PayableFailureResponseAssembler.toResponse(failure, messages));
    }

    private ResponseEntity<?> notFound() {
        return PayableFailureResponseAssembler.toResponse(PayableCommandFailure.PAYABLE_NOT_FOUND, messages);
    }
}
