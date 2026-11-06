package com.brasper.platform.expenses.application.commandservices;

import com.brasper.platform.expenses.application.outboundservices.ExpenseProjectLookup;
import com.brasper.platform.expenses.application.outboundservices.ExpenseTaxRatesProvider;
import com.brasper.platform.expenses.domain.model.aggregates.Expense;
import com.brasper.platform.expenses.domain.model.commands.AnnulExpenseCommand;
import com.brasper.platform.expenses.domain.model.commands.CorrectExpenseCommand;
import com.brasper.platform.expenses.domain.model.commands.RegisterExpenseCommand;
import com.brasper.platform.expenses.domain.model.valueobjects.PaymentMethod;
import com.brasper.platform.expenses.domain.repositories.ExpenseRepository;
import com.brasper.platform.expenses.domain.repositories.SupplierRepository;
import com.brasper.platform.shared.application.result.Result;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pre-checks pick the failure to report; the aggregate enforces the same rules again.
 * shortcut: bank accounts and partners are only checked for presence until Treasury can confirm they exist.
 */
@Service
public class ExpenseCommandService {

    private static final String CANCELLED = "CANCELLED";

    private final ExpenseRepository expenses;
    private final SupplierRepository suppliers;
    private final ExpenseProjectLookup projects;
    private final ExpenseTaxRatesProvider taxRates;

    public ExpenseCommandService(ExpenseRepository expenses, SupplierRepository suppliers,
            ExpenseProjectLookup projects, ExpenseTaxRatesProvider taxRates) {
        this.expenses = expenses;
        this.suppliers = suppliers;
        this.projects = projects;
        this.taxRates = taxRates;
    }

    @Transactional
    public Result<Expense, ExpenseCommandFailure> handle(RegisterExpenseCommand command) {
        Optional<ExpenseCommandFailure> failure = paymentTargetFailure(command)
                .or(() -> projectFailure(command.projectId()))
                .or(() -> supplierFailure(command.supplierId()));
        if (failure.isPresent()) {
            return Result.failure(failure.get());
        }
        return Result.success(expenses.save(new Expense(command, taxRates.ratesAt(command.spentOn()))));
    }

    @Transactional
    public Result<Expense, ExpenseCommandFailure> handle(CorrectExpenseCommand command) {
        Expense expense = expenses.findById(command.expenseId()).orElse(null);
        if (expense == null) {
            return Result.failure(ExpenseCommandFailure.EXPENSE_NOT_FOUND);
        }
        if (expense.isAnnulled()) {
            return Result.failure(ExpenseCommandFailure.ALREADY_ANNULLED);
        }
        // Keeping the current project is always fine, even if it was cancelled since.
        Optional<ExpenseCommandFailure> failure = Objects.equals(command.projectId(), expense.getProjectId())
                ? Optional.empty() : projectFailure(command.projectId());
        if (failure.isPresent()) {
            return Result.failure(failure.get());
        }
        expense.correct(command);
        return Result.success(expenses.save(expense));
    }

    @Transactional
    public Result<Expense, ExpenseCommandFailure> handle(AnnulExpenseCommand command) {
        Expense expense = expenses.findById(command.expenseId()).orElse(null);
        if (expense == null) {
            return Result.failure(ExpenseCommandFailure.EXPENSE_NOT_FOUND);
        }
        if (expense.isAnnulled()) {
            return Result.failure(ExpenseCommandFailure.ALREADY_ANNULLED);
        }
        expense.annul();
        return Result.success(expenses.save(expense));
    }

    private static Optional<ExpenseCommandFailure> paymentTargetFailure(RegisterExpenseCommand command) {
        if (command.paymentMethod() == PaymentMethod.PARTNER_CONTRIBUTION) {
            return command.partnerId() == null ? Optional.of(ExpenseCommandFailure.PARTNER_REQUIRED) : Optional.empty();
        }
        return command.paymentMethod() != null && command.bankAccountId() == null
                ? Optional.of(ExpenseCommandFailure.BANK_ACCOUNT_REQUIRED) : Optional.empty();
    }

    /** A general expense (no project) needs no check; a cancelled project takes no new costs. */
    private Optional<ExpenseCommandFailure> projectFailure(Long projectId) {
        if (projectId == null) {
            return Optional.empty();
        }
        return projects.findStatus(projectId)
                .map(status -> status.equals(CANCELLED) ? Optional.of(ExpenseCommandFailure.PROJECT_CLOSED)
                        : Optional.<ExpenseCommandFailure>empty())
                .orElseGet(() -> Optional.of(ExpenseCommandFailure.PROJECT_NOT_FOUND));
    }

    private Optional<ExpenseCommandFailure> supplierFailure(Long supplierId) {
        return supplierId != null && suppliers.findById(supplierId).isEmpty()
                ? Optional.of(ExpenseCommandFailure.SUPPLIER_NOT_FOUND) : Optional.empty();
    }
}
