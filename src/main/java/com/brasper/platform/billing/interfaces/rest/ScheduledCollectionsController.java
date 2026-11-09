package com.brasper.platform.billing.interfaces.rest;

import com.brasper.platform.billing.application.commandservices.ScheduledCollectionCommandService;
import com.brasper.platform.billing.application.queryservices.ScheduledCollectionQueryService;
import com.brasper.platform.billing.domain.model.commands.CancelScheduledCollectionCommand;
import com.brasper.platform.billing.domain.model.commands.RescheduleCollectionCommand;
import com.brasper.platform.billing.domain.model.commands.ScheduleCollectionCommand;
import com.brasper.platform.billing.interfaces.rest.resources.RescheduleCollectionResource;
import com.brasper.platform.billing.interfaces.rest.resources.ScheduleCollectionResource;
import com.brasper.platform.billing.interfaces.rest.transform.ScheduledCollectionFailureResponseAssembler;
import com.brasper.platform.billing.interfaces.rest.transform.ScheduledCollectionResourceFromEntityAssembler;
import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
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

/** Any signed-in user reads; only admin and accounting change the schedule. */
@RestController
@RequestMapping("/api/v1/scheduled-collections")
@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTING','MANAGEMENT')")
public class ScheduledCollectionsController {

    private static final String CAN_WRITE = "hasAnyRole('ADMIN','ACCOUNTING')";

    private final ScheduledCollectionCommandService commands;
    private final ScheduledCollectionQueryService queries;
    private final MessageSource messages;

    public ScheduledCollectionsController(ScheduledCollectionCommandService commands,
            ScheduledCollectionQueryService queries, MessageSource messages) {
        this.commands = commands;
        this.queries = queries;
        this.messages = messages;
    }

    @PostMapping
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> schedule(@Valid @RequestBody ScheduleCollectionResource resource) {
        var command = new ScheduleCollectionCommand(resource.projectId(),
                new Money(resource.expectedAmount(), resource.currency()), CashFlowWeek.from(resource.expectedOn()),
                resource.notes());
        return commands.handle(command).fold(
                collection -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ScheduledCollectionResourceFromEntityAssembler.toResourceFromEntity(collection)),
                failure -> ScheduledCollectionFailureResponseAssembler.toResponse(failure, messages));
    }

    @PostMapping("/{id}/reschedule")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> reschedule(@PathVariable Long id,
            @Valid @RequestBody RescheduleCollectionResource resource) {
        return commands.handle(new RescheduleCollectionCommand(id, CashFlowWeek.from(resource.expectedOn()))).fold(
                collection -> ResponseEntity.ok(
                        ScheduledCollectionResourceFromEntityAssembler.toResourceFromEntity(collection)),
                failure -> ScheduledCollectionFailureResponseAssembler.toResponse(failure, messages));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        return commands.handle(new CancelScheduledCollectionCommand(id)).fold(
                collection -> ResponseEntity.ok(
                        ScheduledCollectionResourceFromEntityAssembler.toResourceFromEntity(collection)),
                failure -> ScheduledCollectionFailureResponseAssembler.toResponse(failure, messages));
    }

    /** Collections expected in the month, ordered by week. A missing year or month is a 400. */
    @GetMapping
    public ResponseEntity<?> list(@RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(queries.findByPeriod(new Period(year, month)).stream()
                .map(ScheduledCollectionResourceFromEntityAssembler::toResourceFromEntity).toList());
    }
}
