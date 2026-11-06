package com.brasper.platform.treasury.interfaces.rest;

import com.brasper.platform.shared.application.result.Result;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Percentage;
import com.brasper.platform.treasury.application.commandservices.LoanCommandFailure;
import com.brasper.platform.treasury.application.commandservices.LoanCommandService;
import com.brasper.platform.treasury.application.queryservices.LoanQueryService;
import com.brasper.platform.treasury.domain.model.aggregates.Loan;
import com.brasper.platform.treasury.domain.model.commands.PayLoanInstallmentCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterLoanCommand;
import com.brasper.platform.treasury.domain.model.commands.ScheduleLoanInstallmentCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.LoanType;
import com.brasper.platform.treasury.interfaces.rest.resources.PayInstallmentResource;
import com.brasper.platform.treasury.interfaces.rest.resources.RegisterLoanResource;
import com.brasper.platform.treasury.interfaces.rest.resources.ScheduleInstallmentResource;
import com.brasper.platform.treasury.interfaces.rest.transform.LoanFailureResponseAssembler;
import com.brasper.platform.treasury.interfaces.rest.transform.LoanResourceAssembler;
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

/** Any signed-in user reads; loans are written only by admin and accounting. */
@RestController
@RequestMapping("/api/v1/loans")
public class LoansController {

    private static final String CAN_WRITE = "hasAnyRole('ADMIN','ACCOUNTING')";
    private final LoanCommandService commands;
    private final LoanQueryService queries;
    private final MessageSource messages;

    public LoansController(LoanCommandService commands, LoanQueryService queries, MessageSource messages) {
        this.commands = commands;
        this.queries = queries;
        this.messages = messages;
    }

    @PostMapping
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> register(@Valid @RequestBody RegisterLoanResource resource) {
        var command = new RegisterLoanCommand(resource.type(), resource.counterparty(),
                new Money(resource.principal(), resource.currency()), new Percentage(resource.interestRate()),
                resource.disbursedOn(), resource.bankAccountId());
        return respond(commands.handle(command), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) LoanType type) {
        return ResponseEntity.ok(queries.findAll(type).stream().map(LoanResourceAssembler::toResource).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return queries.findById(id)
                .<ResponseEntity<?>>map(loan -> ResponseEntity.ok(LoanResourceAssembler.toResource(loan)))
                .orElseGet(() -> notFound());
    }

    @PostMapping("/{id}/installments")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> schedule(@PathVariable Long id, @Valid @RequestBody ScheduleInstallmentResource resource) {
        return queries.findById(id).<ResponseEntity<?>>map(loan -> {
            var currency = loan.getPrincipal().currency();
            var command = new ScheduleLoanInstallmentCommand(id, resource.dueOn(),
                    new Money(resource.principal(), currency), new Money(resource.interest(), currency));
            return respond(commands.handle(command), HttpStatus.CREATED);
        }).orElseGet(() -> notFound());
    }

    @PostMapping("/{id}/installments/{installmentId}/payments")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> pay(@PathVariable Long id, @PathVariable Long installmentId,
            @Valid @RequestBody PayInstallmentResource resource) {
        return respond(commands.handle(new PayLoanInstallmentCommand(id, installmentId, resource.paidOn(),
                resource.bankAccountId())), HttpStatus.OK);
    }

    private ResponseEntity<?> respond(Result<Loan, LoanCommandFailure> result, HttpStatus status) {
        return result.<ResponseEntity<?>>fold(
                loan -> ResponseEntity.status(status).body(LoanResourceAssembler.toResource(loan)),
                failure -> LoanFailureResponseAssembler.toResponse(failure, messages));
    }

    private ResponseEntity<?> notFound() {
        return LoanFailureResponseAssembler.toResponse(LoanCommandFailure.LOAN_NOT_FOUND, messages);
    }
}
