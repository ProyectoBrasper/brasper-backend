package com.brasper.platform.reporting.interfaces.rest;

import com.brasper.platform.reporting.application.queryservices.FinancialStatementsQueryService;
import com.brasper.platform.reporting.interfaces.rest.transform.FinancialStatementsResourceAssembler;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasAnyRole('ADMIN','MANAGEMENT','ACCOUNTING')")
public class FinancialStatementsController {

    private final FinancialStatementsQueryService queries;

    public FinancialStatementsController(FinancialStatementsQueryService queries) {
        this.queries = queries;
    }

    @GetMapping("/income-statement")
    public ResponseEntity<?> incomeStatement(@RequestParam int fromYear, @RequestParam int fromMonth,
            @RequestParam int toYear, @RequestParam int toMonth) {
        return ResponseEntity.ok(FinancialStatementsResourceAssembler.toResource(
                queries.incomeStatement(new Period(fromYear, fromMonth), new Period(toYear, toMonth))));
    }

    @GetMapping("/balance-sheet")
    public ResponseEntity<?> balanceSheet(@RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(FinancialStatementsResourceAssembler.toResource(
                queries.balanceSheet(new Period(year, month))));
    }
}
