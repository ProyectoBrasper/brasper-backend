package com.brasper.platform.projects.interfaces.rest;

import com.brasper.platform.projects.application.commandservices.ClientCommandFailure;
import com.brasper.platform.projects.application.commandservices.ClientCommandService;
import com.brasper.platform.projects.application.queryservices.ClientQueryService;
import com.brasper.platform.projects.domain.model.commands.CreateClientCommand;
import com.brasper.platform.projects.domain.model.commands.UpdateClientCommand;
import com.brasper.platform.projects.interfaces.rest.resources.CreateClientResource;
import com.brasper.platform.projects.interfaces.rest.resources.UpdateClientResource;
import com.brasper.platform.projects.interfaces.rest.transform.ClientFailureResponseAssembler;
import com.brasper.platform.projects.interfaces.rest.transform.ClientResourceFromEntityAssembler;
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
@RequestMapping("/api/v1/clients")
public class ClientsController {

    private static final String CAN_WRITE = "hasAnyRole('ADMIN','ACCOUNTING','ENGINEER')";

    private final ClientCommandService clientCommands;
    private final ClientQueryService clientQueries;
    private final MessageSource messages;

    public ClientsController(ClientCommandService clientCommands, ClientQueryService clientQueries,
            MessageSource messages) {
        this.clientCommands = clientCommands;
        this.clientQueries = clientQueries;
        this.messages = messages;
    }

    @PostMapping
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> create(@Valid @RequestBody CreateClientResource resource) {
        // A malformed RUC throws IllegalArgumentException, mapped to 400 by the global handler.
        var command = new CreateClientCommand(resource.businessName(), new Ruc(resource.ruc()),
                resource.contactName(), resource.phone(), resource.email());
        return clientCommands.handle(command).fold(
                client -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ClientResourceFromEntityAssembler.toResourceFromEntity(client)),
                failure -> ClientFailureResponseAssembler.toResponse(failure, messages));
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody UpdateClientResource resource) {
        var command = new UpdateClientCommand(id, resource.businessName(), resource.contactName(),
                resource.phone(), resource.email());
        return clientCommands.handle(command).fold(
                client -> ResponseEntity.ok(ClientResourceFromEntityAssembler.toResourceFromEntity(client)),
                failure -> ClientFailureResponseAssembler.toResponse(failure, messages));
    }

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(clientQueries.findAll().stream()
                .map(ClientResourceFromEntityAssembler::toResourceFromEntity).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return clientQueries.findById(id)
                .<ResponseEntity<?>>map(client -> ResponseEntity.ok(
                        ClientResourceFromEntityAssembler.toResourceFromEntity(client)))
                .orElseGet(() -> ClientFailureResponseAssembler.toResponse(ClientCommandFailure.CLIENT_NOT_FOUND,
                        messages));
    }
}
