package com.brasper.platform.accounting.interfaces.rest;
import com.brasper.platform.accounting.application.commandservices.LedgerCommandService;
import com.brasper.platform.accounting.application.queryservices.LedgerQueryService;
import com.brasper.platform.accounting.domain.model.commands.CreateLedgerAccountCommand;
import com.brasper.platform.accounting.domain.model.valueobjects.AccountCode;
import com.brasper.platform.accounting.interfaces.rest.resources.*;
import com.brasper.platform.accounting.interfaces.rest.transform.*;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ledger-accounts")
public class LedgerAccountsController {
    private static final String READ = "hasAnyRole('ADMIN','ACCOUNTING','MANAGEMENT')";
    private final LedgerCommandService commands;
    private final LedgerQueryService queries;
    private final MessageSource messages;
    public LedgerAccountsController(LedgerCommandService commands, LedgerQueryService queries, MessageSource messages) {
        this.commands = commands; this.queries = queries; this.messages = messages;
    }
    @PostMapping @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTING')")
    public ResponseEntity<?> create(@Valid @RequestBody CreateLedgerAccountResource resource) {
        return commands.handle(new CreateLedgerAccountCommand(new AccountCode(resource.code()), resource.name(), resource.type()))
                .<ResponseEntity<?>>fold(account -> ResponseEntity.created(URI.create("/api/v1/ledger-accounts/" + account.getId()))
                        .body(LedgerResourceAssembler.toResource(account)), failure -> LedgerFailureResponseAssembler.toResponse(failure, messages));
    }
    @GetMapping @PreAuthorize(READ)
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(queries.findAllAccounts().stream().map(LedgerResourceAssembler::toResource).toList());
    }
    @GetMapping("/trial-balance") @PreAuthorize(READ)
    public ResponseEntity<?> trialBalance(@RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(LedgerResourceAssembler.toResource(queries.trialBalance(new Period(year, month))));
    }
}
