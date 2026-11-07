package com.brasper.platform.accounting.interfaces.rest;
import com.brasper.platform.accounting.application.commandservices.TaxCommandService;
import com.brasper.platform.accounting.application.queryservices.TaxQueryService;
import com.brasper.platform.accounting.domain.model.commands.*;
import com.brasper.platform.accounting.interfaces.rest.resources.*;
import com.brasper.platform.accounting.interfaces.rest.transform.*;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import com.brasper.platform.shared.interfaces.rest.transform.ProblemDetailAssembler;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tax-returns")
public class TaxReturnsController {
    private static final String WRITE = "hasAnyRole('ADMIN','ACCOUNTING')";
    private static final String READ = "hasAnyRole('ADMIN','ACCOUNTING','MANAGEMENT')";
    private final TaxCommandService commands;
    private final TaxQueryService queries;
    private final MessageSource messages;
    public TaxReturnsController(TaxCommandService commands, TaxQueryService queries, MessageSource messages) {
        this.commands = commands; this.queries = queries; this.messages = messages;
    }
    @PostMapping @PreAuthorize(WRITE)
    public ResponseEntity<?> prepare(@Valid @RequestBody PrepareTaxReturnResource resource) {
        return commands.handle(new PrepareTaxReturnCommand(new Period(resource.year(), resource.month()))).<ResponseEntity<?>>fold(
                taxReturn -> ResponseEntity.created(URI.create("/api/v1/tax-returns?year=" + taxReturn.getPeriod().year()
                        + "&month=" + taxReturn.getPeriod().month())).body(TaxReturnResourceAssembler.toResource(taxReturn)),
                failure -> TaxFailureResponseAssembler.toResponse(failure, messages));
    }
    @PostMapping("/{id}/payment") @PreAuthorize(WRITE)
    public ResponseEntity<?> pay(@PathVariable Long id, @Valid @RequestBody PayTaxReturnResource resource) {
        return commands.handle(new PayTaxReturnCommand(id, resource.paidOn(), resource.bankAccountId())).<ResponseEntity<?>>fold(
                taxReturn -> ResponseEntity.ok(TaxReturnResourceAssembler.toResource(taxReturn)),
                failure -> TaxFailureResponseAssembler.toResponse(failure, messages));
    }
    @GetMapping @PreAuthorize(READ)
    public ResponseEntity<?> get(@RequestParam int year, @RequestParam int month) {
        return queries.findReturnByPeriod(new Period(year, month))
                .<ResponseEntity<?>>map(taxReturn -> ResponseEntity.ok(TaxReturnResourceAssembler.toResource(taxReturn)))
                .orElseGet(() -> ProblemDetailAssembler.notFound(messages, "accounting.error.tax-return-not-found"));
    }
    @GetMapping("/history") @PreAuthorize(READ)
    public ResponseEntity<?> history() {
        return ResponseEntity.ok(queries.findAllReturns().stream().map(TaxReturnResourceAssembler::toResource).toList());
    }
}
