package com.brasper.platform.expenses.application.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brasper.platform.expenses.application.outboundservices.ExpenseProjectLookup;
import com.brasper.platform.expenses.application.outboundservices.ExpenseTaxRatesProvider;
import com.brasper.platform.expenses.application.outboundservices.ExpenseTreasuryLookup;
import com.brasper.platform.expenses.domain.events.ExpenseAnnulledEvent;
import com.brasper.platform.expenses.domain.events.ExpenseRegisteredEvent;
import com.brasper.platform.expenses.domain.model.aggregates.Expense;
import com.brasper.platform.expenses.domain.model.aggregates.Supplier;
import com.brasper.platform.expenses.domain.model.commands.AnnulExpenseCommand;
import com.brasper.platform.expenses.domain.model.commands.CorrectExpenseCommand;
import com.brasper.platform.expenses.domain.model.commands.CreateSupplierCommand;
import com.brasper.platform.expenses.domain.model.commands.RegisterExpenseCommand;
import com.brasper.platform.expenses.domain.model.valueobjects.PaymentMethod;
import com.brasper.platform.expenses.domain.model.valueobjects.SupplierType;
import com.brasper.platform.expenses.domain.model.valueobjects.Voucher;
import com.brasper.platform.expenses.domain.model.valueobjects.VoucherType;
import com.brasper.platform.expenses.domain.repositories.ExpenseRepository;
import com.brasper.platform.expenses.domain.repositories.SupplierRepository;
import com.brasper.platform.shared.domain.model.valueobjects.CostCategory;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ExpenseCommandServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 8, 20);

    private final ExpenseRepository expenses = mock(ExpenseRepository.class);
    private final SupplierRepository suppliers = mock(SupplierRepository.class);
    private final ExpenseProjectLookup projects = mock(ExpenseProjectLookup.class);
    private final ExpenseTaxRatesProvider taxRates = mock(ExpenseTaxRatesProvider.class);
    private final ExpenseTreasuryLookup treasury = mock(ExpenseTreasuryLookup.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ExpenseCommandService service = new ExpenseCommandService(expenses, suppliers, projects, taxRates,
            treasury, events);

    private static RegisterExpenseCommand register(Long projectId, Long supplierId, PaymentMethod method,
            Long account, Long partner) {
        return new RegisterExpenseCommand(DAY, projectId, supplierId, CostCategory.MATERIALS, "cables",
                new Voucher(VoucherType.INVOICE, "F001-1"), new Money(new BigDecimal("1000"), Currency.PEN), false,
                method, account, partner, true);
    }

    private static RegisterExpenseCommand valid() {
        return register(3L, 5L, PaymentMethod.BANK_ACCOUNT, 9L, null);
    }

    private void everythingExists() {
        when(projects.findStatus(3L)).thenReturn(Optional.of("IN_PROGRESS"));
        when(treasury.bankAccountExists(9L)).thenReturn(true);
        when(suppliers.findById(5L)).thenReturn(Optional.of(
                new Supplier(new CreateSupplierCommand("Ferreteria", null, SupplierType.NATURAL_PERSON, null))));
        when(taxRates.ratesAt(DAY)).thenReturn(TaxRates.standard());
        when(expenses.save(any(Expense.class))).thenAnswer(call -> call.getArgument(0));
    }

    private Expense stored() {
        everythingExists();
        Expense expense = new Expense(valid(), TaxRates.standard());
        when(expenses.findById(1L)).thenReturn(Optional.of(expense));
        return expense;
    }

    @Test
    void registersWithTheRatesOfTheExpenseDate() {
        everythingExists();

        Expense expense = service.handle(valid()).success().orElseThrow();

        assertThat(expense.getAmounts().igv().amount()).isEqualByComparingTo("180");
    }

    @Test
    void aGeneralExpenseWithoutSupplierNeedsNoLookups() {
        when(treasury.bankAccountExists(2L)).thenReturn(true);
        when(taxRates.ratesAt(DAY)).thenReturn(TaxRates.standard());
        when(expenses.save(any(Expense.class))).thenAnswer(call -> call.getArgument(0));

        assertThat(service.handle(register(null, null, PaymentMethod.PETTY_CASH, 2L, null)).isSuccess()).isTrue();
        verify(projects, never()).findStatus(any());
        verify(suppliers, never()).findById(any());
    }

    @Test
    void reportsUnknownOrCancelledProjectAndUnknownSupplier() {
        everythingExists();
        when(projects.findStatus(8L)).thenReturn(Optional.empty());
        when(projects.findStatus(9L)).thenReturn(Optional.of("CANCELLED"));

        assertThat(service.handle(register(8L, 5L, PaymentMethod.BANK_ACCOUNT, 9L, null)).failure())
                .contains(ExpenseCommandFailure.PROJECT_NOT_FOUND);
        assertThat(service.handle(register(9L, 5L, PaymentMethod.BANK_ACCOUNT, 9L, null)).failure())
                .contains(ExpenseCommandFailure.PROJECT_CLOSED);
        assertThat(service.handle(register(3L, 6L, PaymentMethod.BANK_ACCOUNT, 9L, null)).failure())
                .contains(ExpenseCommandFailure.SUPPLIER_NOT_FOUND);
        verify(expenses, never()).save(any());
    }

    @Test
    void aCompletedProjectStillTakesLateExpenses() {
        everythingExists();
        when(projects.findStatus(3L)).thenReturn(Optional.of("COMPLETED"));

        assertThat(service.handle(valid()).isSuccess()).isTrue();
    }

    @Test
    void reportsAMissingPaymentTarget() {
        everythingExists();

        assertThat(service.handle(register(3L, 5L, PaymentMethod.BANK_ACCOUNT, null, null)).failure())
                .contains(ExpenseCommandFailure.BANK_ACCOUNT_REQUIRED);
        assertThat(service.handle(register(3L, 5L, PaymentMethod.PETTY_CASH, null, null)).failure())
                .contains(ExpenseCommandFailure.BANK_ACCOUNT_REQUIRED);
        assertThat(service.handle(register(3L, 5L, PaymentMethod.PARTNER_CONTRIBUTION, null, null)).failure())
                .contains(ExpenseCommandFailure.PARTNER_REQUIRED);
        assertThat(service.handle(register(3L, 5L, PaymentMethod.BANK_ACCOUNT, 8L, null)).failure())
                .contains(ExpenseCommandFailure.BANK_ACCOUNT_NOT_FOUND);
        assertThat(service.handle(register(3L, 5L, PaymentMethod.PARTNER_CONTRIBUTION, null, 8L)).failure())
                .contains(ExpenseCommandFailure.PARTNER_NOT_FOUND);
    }

    @Test
    void correctsAnExpenseAndChecksTheNewProject() {
        Expense expense = stored();
        when(projects.findStatus(4L)).thenReturn(Optional.of("IN_PROGRESS"));
        when(projects.findStatus(8L)).thenReturn(Optional.empty());

        Expense corrected = service.handle(new CorrectExpenseCommand(1L, 4L, CostCategory.LABOR, "mano de obra"))
                .success().orElseThrow();

        assertThat(corrected).isSameAs(expense);
        assertThat(corrected.getProjectId()).isEqualTo(4L);
        assertThat(service.handle(new CorrectExpenseCommand(1L, 8L, CostCategory.LABOR, null)).failure())
                .contains(ExpenseCommandFailure.PROJECT_NOT_FOUND);
        assertThat(service.handle(new CorrectExpenseCommand(1L, null, CostCategory.OTHER, null)).isSuccess())
                .isTrue();
    }

    @Test
    void keepingTheSameProjectNeedsNoCheck() {
        stored();
        when(projects.findStatus(3L)).thenReturn(Optional.of("CANCELLED"));

        assertThat(service.handle(new CorrectExpenseCommand(1L, 3L, CostCategory.LABOR, null)).isSuccess())
                .isTrue();
    }

    @Test
    void annulsOnceAndAnAnnulledExpenseCannotBeCorrected() {
        Expense expense = stored();

        assertThat(service.handle(new AnnulExpenseCommand(1L)).isSuccess()).isTrue();
        assertThat(expense.isAnnulled()).isTrue();
        assertThat(service.handle(new AnnulExpenseCommand(1L)).failure())
                .contains(ExpenseCommandFailure.ALREADY_ANNULLED);
        assertThat(service.handle(new CorrectExpenseCommand(1L, 3L, CostCategory.LABOR, null)).failure())
                .contains(ExpenseCommandFailure.ALREADY_ANNULLED);
    }

    @Test
    void changesToAnUnknownExpenseFail() {
        when(expenses.findById(9L)).thenReturn(Optional.empty());

        assertThat(service.handle(new AnnulExpenseCommand(9L)).failure())
                .contains(ExpenseCommandFailure.EXPENSE_NOT_FOUND);
        assertThat(service.handle(new CorrectExpenseCommand(9L, null, CostCategory.OTHER, null)).failure())
                .contains(ExpenseCommandFailure.EXPENSE_NOT_FOUND);
    }

    @Test
    void publishesAnEventWhenAnExpenseIsRegisteredOrAnnulled() {
        everythingExists();
        service.handle(valid());
        verify(events).publishEvent(any(ExpenseRegisteredEvent.class));

        stored();
        service.handle(new AnnulExpenseCommand(1L));
        verify(events).publishEvent(any(ExpenseAnnulledEvent.class));
    }
}
