package com.brasper.platform.expenses.application.commandservices;

import com.brasper.platform.expenses.application.outboundservices.ExpenseProjectLookup;
import com.brasper.platform.expenses.application.outboundservices.ExpenseTaxRatesProvider;
import com.brasper.platform.expenses.application.outboundservices.ExpenseTreasuryLookup;
import com.brasper.platform.expenses.domain.events.ExpenseAnnulledEvent;
import com.brasper.platform.expenses.domain.events.ExpenseRegisteredEvent;
import com.brasper.platform.expenses.domain.model.aggregates.Expense;
import com.brasper.platform.expenses.domain.model.commands.AnnulExpenseCommand;
import com.brasper.platform.expenses.domain.model.commands.CorrectExpenseCommand;
import com.brasper.platform.expenses.domain.model.commands.RegisterExpenseCommand;
import com.brasper.platform.expenses.domain.model.valueobjects.PaymentMethod;
import com.brasper.platform.expenses.domain.repositories.ExpenseRepository;
import com.brasper.platform.expenses.domain.repositories.SupplierRepository;
import com.brasper.platform.shared.application.result.Result;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.PaymentStatus;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pre-checks pick the failure to report; the aggregate enforces the same rules again.
 */
@Service
public class ExpenseCommandService {

    private static final String CANCELLED = "CANCELLED";

    private final ExpenseRepository expenses;
    private final SupplierRepository suppliers;
    private final ExpenseProjectLookup projects;
    private final ExpenseTaxRatesProvider taxRates;
    private final ExpenseTreasuryLookup treasury;
    private final ApplicationEventPublisher events;

    public ExpenseCommandService(ExpenseRepository expenses, SupplierRepository suppliers,
            ExpenseProjectLookup projects, ExpenseTaxRatesProvider taxRates,
            ExpenseTreasuryLookup treasury, ApplicationEventPublisher events) {
        this.expenses = expenses;
        this.suppliers = suppliers;
        this.projects = projects;
        this.taxRates = taxRates;
        this.treasury = treasury;
        this.events = events;
    }

    @Transactional
    public Result<Expense, ExpenseCommandFailure> handle(RegisterExpenseCommand command) {
        // shortcut: reports, IGV and balances sum PEN only; accept USD once an exchange rate is stored with the entry.
        if (command.amount().currency() != Currency.PEN) {
            return Result.failure(ExpenseCommandFailure.UNSUPPORTED_CURRENCY);
        }
        Optional<ExpenseCommandFailure> failure = paymentTargetFailure(command)
                .or(() -> projectFailure(command.projectId()))
                .or(() -> supplierFailure(command.supplierId()));
        if (failure.isPresent()) {
            return Result.failure(failure.get());
        }
        Expense expense = expenses.save(new Expense(command, taxRates.ratesAt(command.spentOn())));
        var amounts = expense.getAmounts();
        events.publishEvent(new ExpenseRegisteredEvent(expense.getId(), expense.getSpentOn(), expense.getProjectId(),
                amounts.taxableBase(), amounts.igv(), amounts.total(), expense.getPaymentStatus() == PaymentStatus.PAID,
                expense.getPaymentMethod(), expense.getBankAccountId(), expense.getPartnerId()));
        return Result.success(expense);
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
        Expense saved = expenses.save(expense);
        events.publishEvent(new ExpenseAnnulledEvent(saved.getId(), LocalDate.now(),
                saved.getPaymentStatus() == PaymentStatus.PAID, saved.getBankAccountId(), saved.getAmounts().total()));
        return Result.success(saved);
    }

    private Optional<ExpenseCommandFailure> paymentTargetFailure(RegisterExpenseCommand command) {
        if (command.paymentMethod() == PaymentMethod.PARTNER_CONTRIBUTION) {
            if (command.partnerId() == null) {
                return Optional.of(ExpenseCommandFailure.PARTNER_REQUIRED);
            }
            return treasury.partnerExists(command.partnerId())
                    ? Optional.empty() : Optional.of(ExpenseCommandFailure.PARTNER_NOT_FOUND);
        }
        if (command.paymentMethod() == null) {
            return Optional.empty();
        }
        if (command.bankAccountId() == null) {
            return Optional.of(ExpenseCommandFailure.BANK_ACCOUNT_REQUIRED);
        }
        return treasury.bankAccountExists(command.bankAccountId())
                ? Optional.empty() : Optional.of(ExpenseCommandFailure.BANK_ACCOUNT_NOT_FOUND);
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
