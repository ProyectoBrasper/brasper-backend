package com.brasper.platform.reporting.interfaces.rest;

import com.brasper.platform.reporting.application.queryservices.ProjectProfitabilityQueryService;
import com.brasper.platform.reporting.interfaces.rest.transform.ProjectProfitabilityResourceAssembler;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import com.brasper.platform.shared.interfaces.rest.transform.ProblemDetailAssembler;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports/projects")
@PreAuthorize("hasAnyRole('ADMIN','MANAGEMENT','ACCOUNTING')")
public class ProjectProfitabilityController {

    private final ProjectProfitabilityQueryService queries;
    private final MessageSource messages;

    public ProjectProfitabilityController(ProjectProfitabilityQueryService queries, MessageSource messages) {
        this.queries = queries;
        this.messages = messages;
    }

    @GetMapping("/{id}/profitability")
    public ResponseEntity<?> project(@PathVariable Long id, @RequestParam int year, @RequestParam int month) {
        return queries.findByProject(id, new Period(year, month))
                .<ResponseEntity<?>>map(p -> ResponseEntity.ok(ProjectProfitabilityResourceAssembler.toResource(p)))
                .orElseGet(() -> ProblemDetailAssembler.notFound(messages, "reporting.error.project-not-found"));
    }

    @GetMapping("/profitability")
    public ResponseEntity<?> portfolio(@RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(queries.findPortfolio(new Period(year, month)).stream()
                .map(ProjectProfitabilityResourceAssembler::toResource).toList());
    }
}
