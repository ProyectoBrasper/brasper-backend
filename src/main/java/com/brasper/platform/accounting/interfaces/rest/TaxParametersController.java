package com.brasper.platform.accounting.interfaces.rest;
import com.brasper.platform.accounting.application.commandservices.TaxCommandService;
import com.brasper.platform.accounting.application.queryservices.TaxQueryService;
import com.brasper.platform.accounting.domain.model.commands.SetTaxParametersCommand;
import com.brasper.platform.accounting.interfaces.rest.resources.SetTaxParametersResource;
import com.brasper.platform.accounting.interfaces.rest.transform.*;
import com.brasper.platform.shared.domain.model.valueobjects.Percentage;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tax-parameters")
public class TaxParametersController {
    private static final String READ = "hasAnyRole('ADMIN','ACCOUNTING','MANAGEMENT')";
    private final TaxCommandService commands;
    private final TaxQueryService queries;
    private final MessageSource messages;
    public TaxParametersController(TaxCommandService commands, TaxQueryService queries, MessageSource messages) {
        this.commands = commands;
        this.queries = queries;
        this.messages = messages;
    }
    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> set(@Valid @RequestBody SetTaxParametersResource resource) {
        var rates = new TaxRates(new Percentage(resource.igv()), new Percentage(resource.detraction()),
                new Percentage(resource.retention()), new Percentage(resource.incomeTaxPrepayment()));
        return commands.handle(new SetTaxParametersCommand(rates, resource.effectiveFrom())).<ResponseEntity<?>>fold(
                parameters -> ResponseEntity.status(201).body(TaxParametersResourceAssembler.toResource(parameters)),
                failure -> TaxFailureResponseAssembler.toResponse(failure, messages));
    }
    @GetMapping @PreAuthorize(READ)
    public ResponseEntity<?> get(@RequestParam(required = false) LocalDate date) {
        return queries.findParametersAt(date == null ? LocalDate.now() : date)
                .<ResponseEntity<?>>map(parameters -> ResponseEntity.ok(TaxParametersResourceAssembler.toResource(parameters)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    @GetMapping("/history") @PreAuthorize(READ)
    public ResponseEntity<?> history() {
        return ResponseEntity.ok(queries.findAllParameters().stream().map(TaxParametersResourceAssembler::toResource).toList());
    }
}
