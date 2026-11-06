package com.brasper.platform.treasury.interfaces.rest;

import com.brasper.platform.shared.application.result.Result;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.treasury.application.commandservices.PartnerCommandFailure;
import com.brasper.platform.treasury.application.commandservices.PartnerCommandService;
import com.brasper.platform.treasury.application.queryservices.PartnerQueryService;
import com.brasper.platform.treasury.domain.model.aggregates.Partner;
import com.brasper.platform.treasury.domain.model.commands.RegisterPartnerCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterPartnerContributionCommand;
import com.brasper.platform.treasury.interfaces.rest.resources.RegisterContributionResource;
import com.brasper.platform.treasury.interfaces.rest.resources.RegisterPartnerResource;
import com.brasper.platform.treasury.interfaces.rest.transform.PartnerFailureResponseAssembler;
import com.brasper.platform.treasury.interfaces.rest.transform.PartnerResourceAssembler;
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

/** Any signed-in user reads; partners and their contributions are written only by admin and accounting. */
@RestController
@RequestMapping("/api/v1/partners")
public class PartnersController {

    private static final String CAN_WRITE = "hasAnyRole('ADMIN','ACCOUNTING')";

    private final PartnerCommandService commands;
    private final PartnerQueryService queries;
    private final MessageSource messages;

    public PartnersController(PartnerCommandService commands, PartnerQueryService queries, MessageSource messages) {
        this.commands = commands;
        this.queries = queries;
        this.messages = messages;
    }

    @PostMapping
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> register(@Valid @RequestBody RegisterPartnerResource resource) {
        return created(commands.handle(new RegisterPartnerCommand(resource.fullName(), resource.role())));
    }

    @PostMapping("/{id}/contributions")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> contribute(@PathVariable Long id, @Valid @RequestBody RegisterContributionResource resource) {
        var command = new RegisterPartnerContributionCommand(id, resource.contributedOn(),
                new Money(resource.amount(), resource.currency()), resource.reimbursable(), resource.bankAccountId(),
                null, resource.concept());
        return created(commands.handle(command));
    }

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(queries.findAll().stream().map(PartnerResourceAssembler::toResource).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return queries.findById(id)
                .<ResponseEntity<?>>map(partner -> ResponseEntity.ok(PartnerResourceAssembler.toResource(partner)))
                .orElseGet(() -> PartnerFailureResponseAssembler.toResponse(
                        PartnerCommandFailure.PARTNER_NOT_FOUND, messages));
    }

    private ResponseEntity<?> created(Result<Partner, PartnerCommandFailure> result) {
        return result.<ResponseEntity<?>>fold(
                partner -> ResponseEntity.status(HttpStatus.CREATED).body(PartnerResourceAssembler.toResource(partner)),
                failure -> PartnerFailureResponseAssembler.toResponse(failure, messages));
    }
}
