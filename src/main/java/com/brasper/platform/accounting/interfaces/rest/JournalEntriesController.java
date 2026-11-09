package com.brasper.platform.accounting.interfaces.rest;
import com.brasper.platform.accounting.application.commandservices.*;
import com.brasper.platform.accounting.application.queryservices.LedgerQueryService;
import com.brasper.platform.accounting.domain.model.aggregates.JournalEntry;
import com.brasper.platform.accounting.domain.model.commands.*;
import com.brasper.platform.accounting.domain.model.valueobjects.*;
import com.brasper.platform.accounting.interfaces.rest.resources.*;
import com.brasper.platform.accounting.interfaces.rest.transform.*;
import com.brasper.platform.shared.application.result.Result;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/journal-entries")
public class JournalEntriesController {
    private static final String WRITE = "hasAnyRole('ADMIN','ACCOUNTING')";
    private static final String READ = "hasAnyRole('ADMIN','ACCOUNTING','MANAGEMENT')";
    private final LedgerCommandService commands;
    private final LedgerQueryService queries;
    private final MessageSource messages;
    public JournalEntriesController(LedgerCommandService commands, LedgerQueryService queries, MessageSource messages) {
        this.commands = commands; this.queries = queries; this.messages = messages;
    }
    @PostMapping @PreAuthorize(WRITE)
    public ResponseEntity<?> register(@Valid @RequestBody RegisterJournalEntryResource resource) {
        return respond(commands.handle(new RegisterJournalEntryCommand(resource.entryDate(), resource.description(), EntrySource.MANUAL, null,
                resource.lines().stream().map(line -> new JournalLineData(new AccountCode(line.accountCode()),
                        new Money(line.debit().amount(), line.debit().currency()), new Money(line.credit().amount(), line.credit().currency()), line.projectId())).toList())));
    }
    @GetMapping @PreAuthorize(READ) @Transactional(readOnly = true)
    public ResponseEntity<?> list(@RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(queries.findEntriesByPeriod(new Period(year, month)).stream().map(LedgerResourceAssembler::toResource).toList());
    }
    @GetMapping("/{id}") @PreAuthorize(READ) @Transactional(readOnly = true)
    public ResponseEntity<?> get(@PathVariable Long id) {
        return queries.findEntryById(id).<ResponseEntity<?>>map(entry -> ResponseEntity.ok(LedgerResourceAssembler.toResource(entry)))
                .orElseGet(() -> LedgerFailureResponseAssembler.toResponse(LedgerCommandFailure.ENTRY_NOT_FOUND, messages));
    }
    @PostMapping("/{id}/reversal") @PreAuthorize(WRITE)
    public ResponseEntity<?> reverse(@PathVariable Long id, @Valid @RequestBody ReverseJournalEntryResource resource) {
        return respond(commands.handle(new ReverseJournalEntryCommand(id, resource.reversedOn())));
    }
    private ResponseEntity<?> respond(Result<JournalEntry, LedgerCommandFailure> result) {
        return result.<ResponseEntity<?>>fold(entry -> ResponseEntity.created(URI.create("/api/v1/journal-entries/" + entry.getId()))
                .body(LedgerResourceAssembler.toResource(entry)), failure -> LedgerFailureResponseAssembler.toResponse(failure, messages));
    }
}
