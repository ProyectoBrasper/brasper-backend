package com.brasper.platform.accounting.interfaces.rest;
import com.brasper.platform.accounting.application.commandservices.*;
import com.brasper.platform.accounting.application.queryservices.FixedAssetQueryService;
import com.brasper.platform.accounting.domain.model.aggregates.FixedAsset;
import com.brasper.platform.accounting.domain.model.commands.*;
import com.brasper.platform.accounting.interfaces.rest.resources.*;
import com.brasper.platform.accounting.interfaces.rest.transform.*;
import com.brasper.platform.shared.application.result.Result;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fixed-assets")
public class FixedAssetsController {
    private static final String WRITE = "hasAnyRole('ADMIN','ACCOUNTING')";
    private static final String READ = "hasAnyRole('ADMIN','ACCOUNTING','MANAGEMENT')";
    private final FixedAssetCommandService commands;
    private final FixedAssetQueryService queries;
    private final MessageSource messages;
    public FixedAssetsController(FixedAssetCommandService commands, FixedAssetQueryService queries, MessageSource messages) {
        this.commands = commands;
        this.queries = queries;
        this.messages = messages;
    }
    @PostMapping @PreAuthorize(WRITE)
    public ResponseEntity<?> register(@Valid @RequestBody RegisterFixedAssetResource resource) {
        return respond(commands.handle(new RegisterFixedAssetCommand(resource.description(), resource.acquiredOn(),
                new Money(resource.taxableBase(), resource.currency()), resource.usefulLifeMonths(), resource.expenseId())), HttpStatus.CREATED);
    }
    @PostMapping("/{id}/disposal") @PreAuthorize(WRITE)
    public ResponseEntity<?> dispose(@PathVariable Long id, @Valid @RequestBody DisposeFixedAssetResource resource) {
        return respond(commands.handle(new DisposeFixedAssetCommand(id, resource.disposedOn())), HttpStatus.OK);
    }
    @GetMapping @PreAuthorize(READ)
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(queries.findAll().stream().map(FixedAssetResourceAssembler::toResource).toList());
    }
    @GetMapping("/{id}") @PreAuthorize(READ)
    public ResponseEntity<?> get(@PathVariable Long id) {
        return queries.findById(id).<ResponseEntity<?>>map(asset -> ResponseEntity.ok(FixedAssetResourceAssembler.toResource(asset)))
                .orElseGet(() -> FixedAssetFailureResponseAssembler.toResponse(FixedAssetCommandFailure.FIXED_ASSET_NOT_FOUND, messages));
    }
    private ResponseEntity<?> respond(Result<FixedAsset, FixedAssetCommandFailure> result, HttpStatus status) {
        return result.<ResponseEntity<?>>fold(asset -> ResponseEntity.status(status).body(FixedAssetResourceAssembler.toResource(asset)),
                failure -> FixedAssetFailureResponseAssembler.toResponse(failure, messages));
    }
}
