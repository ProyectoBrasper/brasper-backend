package com.brasper.platform.reporting.interfaces.rest;

import com.brasper.platform.reporting.application.queryservices.DashboardQueryService;
import com.brasper.platform.reporting.domain.model.readmodels.Dashboard;
import com.brasper.platform.reporting.interfaces.rest.resources.DashboardResource;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports/dashboard")
@PreAuthorize("hasAnyRole('ADMIN','MANAGEMENT','ACCOUNTING')")
public class DashboardController {

    private final DashboardQueryService queries;

    public DashboardController(DashboardQueryService queries) {
        this.queries = queries;
    }

    @GetMapping
    public ResponseEntity<?> dashboard(@RequestParam int year, @RequestParam int month) {
        Dashboard d = queries.dashboard(new Period(year, month), LocalDate.now());
        return ResponseEntity.ok(new DashboardResource(d.cashBalance(), d.receivablesOutstanding(),
                d.payablesOutstanding(), d.netProfitYtd(), d.projectedClosingBalance(), d.projectsInProgress(),
                d.alerts()));
    }
}
