package com.brasper.platform.projects.interfaces.rest;

import com.brasper.platform.projects.application.commandservices.ProjectCommandFailure;
import com.brasper.platform.projects.application.commandservices.ProjectCommandService;
import com.brasper.platform.projects.application.queryservices.ProjectQueryService;
import com.brasper.platform.projects.domain.model.aggregates.Project;
import com.brasper.platform.projects.domain.model.commands.CancelProjectCommand;
import com.brasper.platform.projects.domain.model.commands.CreateProjectCommand;
import com.brasper.platform.projects.domain.model.commands.StartProjectCommand;
import com.brasper.platform.projects.domain.model.commands.UpdateProjectCommand;
import com.brasper.platform.projects.domain.model.valueobjects.ProjectCode;
import com.brasper.platform.projects.domain.model.valueobjects.ProjectStatus;
import com.brasper.platform.projects.interfaces.rest.resources.CreateProjectResource;
import com.brasper.platform.projects.interfaces.rest.resources.UpdateProjectResource;
import com.brasper.platform.projects.interfaces.rest.transform.ProjectFailureResponseAssembler;
import com.brasper.platform.projects.interfaces.rest.transform.ProjectResourceFromEntityAssembler;
import com.brasper.platform.shared.application.result.Result;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Percentage;
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

/** Any signed-in user reads; management is read-only, so writes need admin, accounting or engineer. */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectsController {

    private static final String CAN_WRITE = "hasAnyRole('ADMIN','ACCOUNTING','ENGINEER')";

    private final ProjectCommandService projectCommands;
    private final ProjectQueryService projectQueries;
    private final MessageSource messages;

    public ProjectsController(ProjectCommandService projectCommands, ProjectQueryService projectQueries,
            MessageSource messages) {
        this.projectCommands = projectCommands;
        this.projectQueries = projectQueries;
        this.messages = messages;
    }

    @PostMapping
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> create(@Valid @RequestBody CreateProjectResource resource) {
        // A malformed code throws IllegalArgumentException, mapped to 400 by the global handler.
        var command = new CreateProjectCommand(new ProjectCode(resource.code()), resource.name(),
                resource.description(), resource.clientId(), new Money(resource.budget(), resource.currency()),
                resource.quotedOn(), new Percentage(resource.participation()),
                new Percentage(resource.salesCommissionRate()));
        return projectCommands.handle(command).fold(
                project -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ProjectResourceFromEntityAssembler.toResourceFromEntity(project)),
                failure -> ProjectFailureResponseAssembler.toResponse(failure, messages));
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody UpdateProjectResource resource) {
        var command = new UpdateProjectCommand(id, resource.name(), resource.description(), resource.budget(),
                new Percentage(resource.participation()), new Percentage(resource.salesCommissionRate()));
        return respond(projectCommands.handle(command));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> start(@PathVariable Long id) {
        return respond(projectCommands.handle(new StartProjectCommand(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        return respond(projectCommands.handle(new CancelProjectCommand(id)));
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(projectQueries.findAll(status, clientId).stream()
                .map(ProjectResourceFromEntityAssembler::toResourceFromEntity).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return projectQueries.findById(id)
                .<ResponseEntity<?>>map(project -> ResponseEntity.ok(
                        ProjectResourceFromEntityAssembler.toResourceFromEntity(project)))
                .orElseGet(() -> ProjectFailureResponseAssembler.toResponse(
                        ProjectCommandFailure.PROJECT_NOT_FOUND, messages));
    }

    private ResponseEntity<?> respond(Result<Project, ProjectCommandFailure> result) {
        return result.fold(
                project -> ResponseEntity.ok(ProjectResourceFromEntityAssembler.toResourceFromEntity(project)),
                failure -> ProjectFailureResponseAssembler.toResponse(failure, messages));
    }
}
