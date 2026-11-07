package com.brasper.platform.reporting.interfaces.rest;

import com.brasper.platform.reporting.application.queryservices.CashFlowQueryService;
import com.brasper.platform.reporting.interfaces.rest.transform.CashFlowReportResourceAssembler;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports/cash-flow")
@PreAuthorize("hasAnyRole('ADMIN','MANAGEMENT','ACCOUNTING')")
public class CashFlowController {

    private final CashFlowQueryService queries;

    public CashFlowController(CashFlowQueryService queries) {
        this.queries = queries;
    }

    @GetMapping("/weekly")
    public ResponseEntity<?> weekly(@RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(CashFlowReportResourceAssembler.toResource(
                queries.weekly(new Period(year, month), LocalDate.now())));
    }

    @GetMapping("/monthly")
    public ResponseEntity<?> monthly(@RequestParam int fromYear, @RequestParam int fromMonth,
            @RequestParam int toYear, @RequestParam int toMonth) {
        return ResponseEntity.ok(CashFlowReportResourceAssembler.toResource(
                queries.monthly(new Period(fromYear, fromMonth), new Period(toYear, toMonth), LocalDate.now())));
    }

    @GetMapping("/projection")
    public ResponseEntity<?> projection(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam int weeks) {
        return ResponseEntity.ok(CashFlowReportResourceAssembler.toResource(
                queries.projection(from, weeks, LocalDate.now())));
    }
}
