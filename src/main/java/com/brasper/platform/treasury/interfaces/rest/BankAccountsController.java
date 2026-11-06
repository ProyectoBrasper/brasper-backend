package com.brasper.platform.treasury.interfaces.rest;

import com.brasper.platform.shared.domain.model.valueobjects.Period;
import com.brasper.platform.treasury.application.commandservices.BankAccountCommandFailure;
import com.brasper.platform.treasury.application.queryservices.BankAccountQueryService;
import com.brasper.platform.treasury.domain.model.queries.GetCashMovementsByAccountAndPeriodQuery;
import com.brasper.platform.treasury.interfaces.rest.transform.BankAccountFailureResponseAssembler;
import com.brasper.platform.treasury.interfaces.rest.transform.BankAccountResourceAssembler;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Any signed-in user reads. */
@RestController
@RequestMapping("/api/v1/bank-accounts")
public class BankAccountsController {

    private final BankAccountQueryService queries;
    private final MessageSource messages;

    public BankAccountsController(BankAccountQueryService queries, MessageSource messages) {
        this.queries = queries;
        this.messages = messages;
    }

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(queries.findAll().stream().map(BankAccountResourceAssembler::toResource).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return queries.findById(id)
                .<ResponseEntity<?>>map(summary -> ResponseEntity.ok(BankAccountResourceAssembler.toResource(summary)))
                .orElseGet(this::notFound);
    }

    @GetMapping("/{id}/movements")
    public ResponseEntity<?> movements(@PathVariable Long id, @RequestParam int year, @RequestParam int month) {
        // An invalid month throws IllegalArgumentException, mapped to 400 by the global handler.
        return queries.handle(new GetCashMovementsByAccountAndPeriodQuery(id, new Period(year, month)))
                .<ResponseEntity<?>>map(list -> ResponseEntity.ok(
                        list.stream().map(BankAccountResourceAssembler::toResource).toList()))
                .orElseGet(this::notFound);
    }

    private ResponseEntity<?> notFound() {
        return BankAccountFailureResponseAssembler.toResponse(BankAccountCommandFailure.BANK_ACCOUNT_NOT_FOUND, messages);
    }
}
