package com.brasper.platform.expenses.interfaces.rest;

import com.brasper.platform.expenses.application.commandservices.ExpenseCommandService;
import com.brasper.platform.expenses.domain.model.commands.AnnulExpenseCommand;
import com.brasper.platform.expenses.domain.model.commands.CorrectExpenseCommand;
import com.brasper.platform.expenses.domain.model.commands.RegisterExpenseCommand;
import com.brasper.platform.expenses.domain.model.valueobjects.Voucher;
import com.brasper.platform.expenses.interfaces.rest.resources.CorrectExpenseResource;
import com.brasper.platform.expenses.interfaces.rest.resources.RegisterExpenseResource;
import com.brasper.platform.expenses.interfaces.rest.transform.ExpenseFailureResponseAssembler;
import com.brasper.platform.expenses.interfaces.rest.transform.ExpenseResourceFromEntityAssembler;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Management is read-only, so writes need admin, accounting or engineer. */
@RestController
@RequestMapping("/api/v1/expenses")
public class ExpensesController {

    private static final String CAN_WRITE = "hasAnyRole('ADMIN','ACCOUNTING','ENGINEER')";

    private final ExpenseCommandService expenseCommands;
    private final MessageSource messages;

    public ExpensesController(ExpenseCommandService expenseCommands, MessageSource messages) {
        this.expenseCommands = expenseCommands;
        this.messages = messages;
    }

    @PostMapping
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> register(@Valid @RequestBody RegisterExpenseResource resource) {
        // A voucher without number (or NO_VOUCHER with one) throws IllegalArgumentException, mapped to 400.
        var command = new RegisterExpenseCommand(resource.spentOn(), resource.projectId(), resource.supplierId(),
                resource.category(), resource.description(),
                new Voucher(resource.voucherType(), resource.voucherNumber()),
                new Money(resource.amount(), resource.currency()), resource.amountIncludesIgv(),
                resource.paymentMethod(), resource.bankAccountId(), resource.partnerId(), resource.paid());
        return expenseCommands.handle(command).fold(
                expense -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ExpenseResourceFromEntityAssembler.toResourceFromEntity(expense)),
                failure -> ExpenseFailureResponseAssembler.toResponse(failure, messages));
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> correct(@PathVariable Long id, @Valid @RequestBody CorrectExpenseResource resource) {
        var command = new CorrectExpenseCommand(id, resource.projectId(), resource.category(),
                resource.description());
        return expenseCommands.handle(command).fold(
                expense -> ResponseEntity.ok(ExpenseResourceFromEntityAssembler.toResourceFromEntity(expense)),
                failure -> ExpenseFailureResponseAssembler.toResponse(failure, messages));
    }

    @PostMapping("/{id}/annul")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> annul(@PathVariable Long id) {
        return expenseCommands.handle(new AnnulExpenseCommand(id)).fold(
                expense -> ResponseEntity.ok(ExpenseResourceFromEntityAssembler.toResourceFromEntity(expense)),
                failure -> ExpenseFailureResponseAssembler.toResponse(failure, messages));
    }
}
