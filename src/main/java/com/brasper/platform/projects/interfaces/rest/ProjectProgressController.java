package com.brasper.platform.projects.interfaces.rest;

import com.brasper.platform.projects.application.commandservices.ProjectCommandFailure;
import com.brasper.platform.projects.application.commandservices.ProjectCommandService;
import com.brasper.platform.projects.application.queryservices.ProjectQueryService;
import com.brasper.platform.projects.domain.model.commands.RegisterProjectProgressCommand;
import com.brasper.platform.projects.interfaces.rest.resources.RegisterProgressResource;
import com.brasper.platform.projects.interfaces.rest.transform.ProgressRecordResourceFromEntityAssembler;
import com.brasper.platform.projects.interfaces.rest.transform.ProjectFailureResponseAssembler;
import com.brasper.platform.shared.domain.model.valueobjects.Percentage;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/progress")
public class ProjectProgressController {

    private final ProjectCommandService projectCommands;
    private final ProjectQueryService projectQueries;
    private final MessageSource messages;

    public ProjectProgressController(ProjectCommandService projectCommands, ProjectQueryService projectQueries,
            MessageSource messages) {
        this.projectCommands = projectCommands;
        this.projectQueries = projectQueries;
        this.messages = messages;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTING','ENGINEER')")
    public ResponseEntity<?> register(@PathVariable Long projectId, @Valid @RequestBody RegisterProgressResource resource) {
        var command = new RegisterProjectProgressCommand(projectId, new Period(resource.year(), resource.month()),
                new Percentage(resource.percentage()));
        return projectCommands.handle(command).fold(
                record -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ProgressRecordResourceFromEntityAssembler.toResourceFromEntity(record)),
                failure -> ProjectFailureResponseAssembler.toResponse(failure, messages));
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable Long projectId) {
        return projectQueries.findProgress(projectId)
                .<ResponseEntity<?>>map(records -> ResponseEntity.ok(records.stream()
                        .map(ProgressRecordResourceFromEntityAssembler::toResourceFromEntity).toList()))
                .orElseGet(() -> ProjectFailureResponseAssembler.toResponse(
                        ProjectCommandFailure.PROJECT_NOT_FOUND, messages));
    }
}
