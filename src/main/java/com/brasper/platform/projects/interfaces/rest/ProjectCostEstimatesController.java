package com.brasper.platform.projects.interfaces.rest;

import com.brasper.platform.projects.application.commandservices.ProjectCommandFailure;
import com.brasper.platform.projects.application.commandservices.ProjectCommandService;
import com.brasper.platform.projects.application.queryservices.ProjectQueryService;
import com.brasper.platform.projects.domain.model.commands.SetProjectCostEstimateCommand;
import com.brasper.platform.projects.interfaces.rest.resources.SetCostEstimateResource;
import com.brasper.platform.projects.interfaces.rest.transform.CostEstimateResourceFromEntityAssembler;
import com.brasper.platform.projects.interfaces.rest.transform.ProjectFailureResponseAssembler;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/cost-estimates")
public class ProjectCostEstimatesController {

    private final ProjectCommandService projectCommands;
    private final ProjectQueryService projectQueries;
    private final MessageSource messages;

    public ProjectCostEstimatesController(ProjectCommandService projectCommands, ProjectQueryService projectQueries,
            MessageSource messages) {
        this.projectCommands = projectCommands;
        this.projectQueries = projectQueries;
        this.messages = messages;
    }

    /** PUT because setting a category again replaces its estimate. */
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTING','ENGINEER')")
    public ResponseEntity<?> set(@PathVariable Long projectId, @Valid @RequestBody SetCostEstimateResource resource) {
        var command = new SetProjectCostEstimateCommand(projectId, resource.category(),
                new Money(resource.estimatedAmount(), resource.currency()));
        return projectCommands.handle(command).fold(
                estimate -> ResponseEntity.ok(CostEstimateResourceFromEntityAssembler.toResourceFromEntity(estimate)),
                failure -> ProjectFailureResponseAssembler.toResponse(failure, messages));
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable Long projectId) {
        return projectQueries.findCostEstimates(projectId)
                .<ResponseEntity<?>>map(estimates -> ResponseEntity.ok(estimates.stream()
                        .map(CostEstimateResourceFromEntityAssembler::toResourceFromEntity).toList()))
                .orElseGet(() -> ProjectFailureResponseAssembler.toResponse(
                        ProjectCommandFailure.PROJECT_NOT_FOUND, messages));
    }
}
