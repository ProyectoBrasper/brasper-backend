package com.brasper.platform.treasury.interfaces.rest;

import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import com.brasper.platform.shared.application.result.Result;
import com.brasper.platform.treasury.application.commandservices.BankAccountCommandFailure;
import com.brasper.platform.treasury.application.commandservices.BankAccountCommandService;
import com.brasper.platform.treasury.application.queryservices.BankAccountQueryService;
import com.brasper.platform.treasury.domain.model.aggregates.BankAccount;
import com.brasper.platform.treasury.domain.model.commands.OpenBankAccountCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterCashMovementCommand;
import com.brasper.platform.treasury.domain.model.commands.TransferFundsCommand;
import com.brasper.platform.treasury.domain.model.queries.GetCashMovementsByAccountAndPeriodQuery;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountNumber;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementSource;
import com.brasper.platform.treasury.interfaces.rest.resources.OpenBankAccountResource;
import com.brasper.platform.treasury.interfaces.rest.resources.RegisterAdjustmentResource;
import com.brasper.platform.treasury.interfaces.rest.resources.TransferFundsResource;
import com.brasper.platform.treasury.interfaces.rest.transform.BankAccountFailureResponseAssembler;
import com.brasper.platform.treasury.interfaces.rest.transform.BankAccountResourceAssembler;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Any signed-in user reads; cash movements are written only by admin and accounting. */
@RestController
@RequestMapping("/api/v1/bank-accounts")
public class BankAccountsController {

    private static final String CAN_WRITE = "hasAnyRole('ADMIN','ACCOUNTING')";

    private final BankAccountCommandService commands;
    private final BankAccountQueryService queries;
    private final MessageSource messages;

    public BankAccountsController(BankAccountCommandService commands, BankAccountQueryService queries,
            MessageSource messages) {
        this.commands = commands;
        this.queries = queries;
        this.messages = messages;
    }

    @PostMapping
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> open(@Valid @RequestBody OpenBankAccountResource resource) {
        var command = new OpenBankAccountCommand(resource.bank(), new AccountNumber(resource.accountNumber()),
                resource.type(), resource.currency(), new Money(resource.openingBalance(), resource.currency()),
                resource.openedOn());
        return created(commands.handle(command));
    }

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(queries.findAll().stream().map(BankAccountResourceAssembler::toResource).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return queries.findById(id)
                .<ResponseEntity<?>>map(summary -> ResponseEntity.ok(BankAccountResourceAssembler.toResource(summary)))
                .orElseGet(() -> notFound());
    }

    @GetMapping("/{id}/movements")
    public ResponseEntity<?> movements(@PathVariable Long id, @RequestParam int year, @RequestParam int month) {
        // An invalid month throws IllegalArgumentException, mapped to 400 by the global handler.
        return queries.handle(new GetCashMovementsByAccountAndPeriodQuery(id, new Period(year, month)))
                .<ResponseEntity<?>>map(list -> ResponseEntity.ok(
                        list.stream().map(BankAccountResourceAssembler::toResource).toList()))
                .orElseGet(() -> notFound());
    }

    @PostMapping("/{id}/adjustments")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> adjust(@PathVariable Long id, @Valid @RequestBody RegisterAdjustmentResource resource) {
        return queries.findById(id).<ResponseEntity<?>>map(summary -> {
            var money = new Money(resource.amount(), summary.account().getCurrency());
            var command = new RegisterCashMovementCommand(id, resource.occurredOn(), resource.direction(), money,
                    resource.concept(), MovementSource.ADJUSTMENT, null);
            return created(commands.handle(command));
        }).orElseGet(() -> notFound());
    }

    @PostMapping("/transfers")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> transfer(@Valid @RequestBody TransferFundsResource resource) {
        return queries.findById(resource.fromAccountId()).<ResponseEntity<?>>map(origin -> {
            var money = new Money(resource.amount(), origin.account().getCurrency());
            var command = new TransferFundsCommand(resource.fromAccountId(), resource.toAccountId(),
                    resource.transferredOn(), money, resource.concept());
            return created(commands.handle(command));
        }).orElseGet(() -> notFound());
    }

    /** Answers with the account as {@code queries} sees it, because its lazy movements need a transaction. */
    private ResponseEntity<?> created(Result<BankAccount, BankAccountCommandFailure> result) {
        return result.<ResponseEntity<?>>fold(
                account -> queries.findById(account.getId())
                        .<ResponseEntity<?>>map(summary -> ResponseEntity.status(HttpStatus.CREATED)
                                .body(BankAccountResourceAssembler.toResource(summary)))
                        .orElseGet(() -> notFound()),
                failure -> BankAccountFailureResponseAssembler.toResponse(failure, messages));
    }

    private ResponseEntity<?> notFound() {
        return BankAccountFailureResponseAssembler.toResponse(BankAccountCommandFailure.BANK_ACCOUNT_NOT_FOUND, messages);
    }
}
