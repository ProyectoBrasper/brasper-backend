package com.brasper.platform.expenses.interfaces.rest;

import com.brasper.platform.expenses.application.commandservices.SupplierCommandFailure;
import com.brasper.platform.expenses.application.commandservices.SupplierCommandService;
import com.brasper.platform.expenses.application.queryservices.SupplierQueryService;
import com.brasper.platform.expenses.domain.model.commands.CreateSupplierCommand;
import com.brasper.platform.expenses.domain.model.commands.UpdateSupplierCommand;
import com.brasper.platform.expenses.interfaces.rest.resources.CreateSupplierResource;
import com.brasper.platform.expenses.interfaces.rest.resources.UpdateSupplierResource;
import com.brasper.platform.expenses.interfaces.rest.transform.SupplierFailureResponseAssembler;
import com.brasper.platform.expenses.interfaces.rest.transform.SupplierResourceFromEntityAssembler;
import com.brasper.platform.shared.domain.model.valueobjects.Ruc;
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
import org.springframework.web.bind.annotation.RestController;

/** Any signed-in user reads; management is read-only, so writes need admin, accounting or engineer. */
@RestController
@RequestMapping("/api/v1/suppliers")
public class SuppliersController {

    private static final String CAN_WRITE = "hasAnyRole('ADMIN','ACCOUNTING','ENGINEER')";

    private final SupplierCommandService supplierCommands;
    private final SupplierQueryService supplierQueries;
    private final MessageSource messages;

    public SuppliersController(SupplierCommandService supplierCommands, SupplierQueryService supplierQueries,
            MessageSource messages) {
        this.supplierCommands = supplierCommands;
        this.supplierQueries = supplierQueries;
        this.messages = messages;
    }

    @PostMapping
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> create(@Valid @RequestBody CreateSupplierResource resource) {
        // A malformed RUC throws IllegalArgumentException, mapped to 400 by the global handler.
        Ruc ruc = resource.ruc() == null || resource.ruc().isBlank() ? null : new Ruc(resource.ruc());
        var command = new CreateSupplierCommand(resource.businessName(), ruc, resource.type(), resource.phone());
        return supplierCommands.handle(command).fold(
                supplier -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(SupplierResourceFromEntityAssembler.toResourceFromEntity(supplier)),
                failure -> SupplierFailureResponseAssembler.toResponse(failure, messages));
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody UpdateSupplierResource resource) {
        var command = new UpdateSupplierCommand(id, resource.businessName(), resource.phone());
        return supplierCommands.handle(command).fold(
                supplier -> ResponseEntity.ok(SupplierResourceFromEntityAssembler.toResourceFromEntity(supplier)),
                failure -> SupplierFailureResponseAssembler.toResponse(failure, messages));
    }

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(supplierQueries.findAll().stream()
                .map(SupplierResourceFromEntityAssembler::toResourceFromEntity).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return supplierQueries.findById(id)
                .<ResponseEntity<?>>map(supplier -> ResponseEntity.ok(
                        SupplierResourceFromEntityAssembler.toResourceFromEntity(supplier)))
                .orElseGet(() -> SupplierFailureResponseAssembler.toResponse(
                        SupplierCommandFailure.SUPPLIER_NOT_FOUND, messages));
    }
}
