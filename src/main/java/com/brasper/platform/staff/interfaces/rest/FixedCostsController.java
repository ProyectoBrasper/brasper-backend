package com.brasper.platform.staff.interfaces.rest;

import com.brasper.platform.shared.application.result.Result;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import com.brasper.platform.staff.application.commandservices.*;
import com.brasper.platform.staff.application.queryservices.FixedCostQueryService;
import com.brasper.platform.staff.domain.model.aggregates.FixedCost;
import com.brasper.platform.staff.domain.model.commands.*;
import com.brasper.platform.staff.domain.model.valueobjects.*;
import com.brasper.platform.staff.interfaces.rest.resources.*;
import com.brasper.platform.staff.interfaces.rest.transform.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fixed-costs")
@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTING','MANAGEMENT')")
// Keep lazy collections available while assembling command responses.
@Transactional
public class FixedCostsController {
    private static final String CAN_WRITE = "hasAnyRole('ADMIN','ACCOUNTING')";
    private final FixedCostCommandService commands;
    private final FixedCostQueryService queries;
    private final MessageSource messages;
    public FixedCostsController(FixedCostCommandService commands, FixedCostQueryService queries, MessageSource messages) {
        this.commands = commands; this.queries = queries; this.messages = messages;
    }
    @PostMapping
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> create(@Valid @RequestBody CreateFixedCostResource r) {
        return respond(commands.handle(new CreateFixedCostCommand(new CostCode(r.code()), r.concept(), r.type(),
                new Money(r.monthlyAmount(), r.currency()), r.employeeId(), new PaymentSchedule(r.expectedWeeks()))), HttpStatus.CREATED);
    }
    @PutMapping("/{id}")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody UpdateFixedCostResource r) {
        return queries.findById(id).<ResponseEntity<?>>map(cost -> respond(commands.handle(new UpdateFixedCostCommand(
                id, r.concept(), new Money(r.monthlyAmount(), cost.getCurrency()), new PaymentSchedule(r.expectedWeeks()))),
                HttpStatus.OK)).orElseGet(this::notFound);
    }
    @PostMapping("/{id}/deactivate")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> deactivate(@PathVariable Long id) {
        return respond(commands.handle(new DeactivateFixedCostCommand(id)), HttpStatus.OK);
    }
    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(queries.findAll(active).stream().map(FixedCostResourceAssembler::toResource).toList());
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return queries.findById(id).<ResponseEntity<?>>map(cost -> ResponseEntity.ok(FixedCostResourceAssembler.toResource(cost)))
                .orElseGet(this::notFound);
    }
    @PostMapping("/{id}/payments")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> pay(@PathVariable Long id, @Valid @RequestBody RegisterFixedCostPaymentResource r) {
        return queries.findById(id).<ResponseEntity<?>>map(cost -> commands.handle(new RegisterFixedCostPaymentCommand(
                id, r.paidOn(), new Period(r.year(), r.month()), new Money(r.amount(), cost.getCurrency()), r.bankAccountId(), r.notes()))
                .<ResponseEntity<?>>fold(saved -> ResponseEntity.status(HttpStatus.CREATED).body(
                        FixedCostResourceAssembler.toResource(saved.getId(), saved.getPayments().getLast())),
                        failure -> FixedCostFailureResponseAssembler.toResponse(failure, messages))).orElseGet(this::notFound);
    }
    @GetMapping("/payments")
    public ResponseEntity<?> payments(@RequestParam @Min(1) int year, @RequestParam @Min(1) @Max(12) int month) {
        return ResponseEntity.ok(queries.findPaymentsByPeriod(new Period(year, month)).stream()
                .map(view -> FixedCostResourceAssembler.toResource(view.fixedCostId(), view.payment())).toList());
    }
    private ResponseEntity<?> respond(Result<FixedCost, FixedCostCommandFailure> result, HttpStatus status) {
        return result.<ResponseEntity<?>>fold(cost -> ResponseEntity.status(status).body(FixedCostResourceAssembler.toResource(cost)),
                failure -> FixedCostFailureResponseAssembler.toResponse(failure, messages));
    }
    private ResponseEntity<?> notFound() {
        return FixedCostFailureResponseAssembler.toResponse(FixedCostCommandFailure.FIXED_COST_NOT_FOUND, messages);
    }
}
