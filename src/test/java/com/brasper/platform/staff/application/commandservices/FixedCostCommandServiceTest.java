package com.brasper.platform.staff.application.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import com.brasper.platform.staff.application.outboundservices.StaffTreasuryLookup;
import com.brasper.platform.staff.domain.model.aggregates.Employee;
import com.brasper.platform.staff.domain.model.aggregates.FixedCost;
import com.brasper.platform.staff.domain.model.commands.CreateFixedCostCommand;
import com.brasper.platform.staff.domain.model.commands.DeactivateFixedCostCommand;
import com.brasper.platform.staff.domain.model.commands.RegisterFixedCostPaymentCommand;
import com.brasper.platform.staff.domain.model.commands.UpdateFixedCostCommand;
import com.brasper.platform.staff.domain.model.valueobjects.CostCode;
import com.brasper.platform.staff.domain.model.valueobjects.FixedCostType;
import com.brasper.platform.staff.domain.model.valueobjects.PaymentSchedule;
import com.brasper.platform.staff.domain.repositories.EmployeeRepository;
import com.brasper.platform.staff.domain.repositories.FixedCostRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class FixedCostCommandServiceTest {

    private static final Money AMOUNT = new Money(new BigDecimal("100"), Currency.PEN);
    private static final PaymentSchedule SCHEDULE = new PaymentSchedule(List.of(1, 3));
    private final FixedCostRepository costs = mock(FixedCostRepository.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final StaffTreasuryLookup treasury = mock(StaffTreasuryLookup.class);
    private final FixedCostCommandService service = new FixedCostCommandService(costs, employees, treasury,
            mock(ApplicationEventPublisher.class));

    private static CreateFixedCostCommand create(FixedCostType type, Long employeeId) {
        return new CreateFixedCostCommand(new CostCode("FC-1"), "Monthly cost", type, AMOUNT, employeeId, SCHEDULE);
    }

    private static RegisterFixedCostPaymentCommand payment(Long accountId) {
        return new RegisterFixedCostPaymentCommand(1L, LocalDate.of(2026, 11, 6), new Period(2026, 11),
                AMOUNT, accountId, "November");
    }

    private FixedCost stored() {
        FixedCost cost = new FixedCost(create(FixedCostType.HOSTING, null));
        when(costs.findById(1L)).thenReturn(Optional.of(cost));
        when(costs.save(any(FixedCost.class))).thenAnswer(call -> call.getArgument(0));
        return cost;
    }

    @Test
    void createsACostWithoutAnEmployeeAndRejectsADuplicateCode() {
        when(costs.save(any(FixedCost.class))).thenAnswer(call -> call.getArgument(0));
        CreateFixedCostCommand command = create(FixedCostType.HOSTING, null);
        when(costs.existsByCode(command.code())).thenReturn(false, true);

        assertThat(service.handle(command).success().orElseThrow().getCode()).isEqualTo(command.code());
        assertThat(service.handle(command).failure()).contains(FixedCostCommandFailure.DUPLICATE_CODE);
        verify(employees, never()).findById(any());
    }

    @Test
    void rejectsACostInUsd() {
        var usd = new CreateFixedCostCommand(new CostCode("FC-1"), "Monthly cost", FixedCostType.HOSTING,
                new Money(new BigDecimal("100"), Currency.USD), null, SCHEDULE);

        assertThat(service.handle(usd).failure()).contains(FixedCostCommandFailure.UNSUPPORTED_CURRENCY);
        verify(costs, never()).save(any());
    }

    @Test
    void createsCostsWithAnExistingEmployeeForAnyType() {
        when(employees.findById(5L)).thenReturn(Optional.of(mock(Employee.class)));
        when(costs.save(any(FixedCost.class))).thenAnswer(call -> call.getArgument(0));

        assertThat(service.handle(create(FixedCostType.SALARY, 5L)).success().orElseThrow().getEmployeeId())
                .isEqualTo(5L);
        assertThat(service.handle(create(FixedCostType.HOSTING, 5L)).isSuccess()).isTrue();
    }

    @Test
    void rejectsAMissingRequiredOrUnknownEmployee() {
        assertThat(service.handle(create(FixedCostType.SALARY, null)).failure())
                .contains(FixedCostCommandFailure.EMPLOYEE_REQUIRED);
        assertThat(service.handle(create(FixedCostType.SALARY, 9L)).failure())
                .contains(FixedCostCommandFailure.EMPLOYEE_NOT_FOUND);
        assertThat(service.handle(create(FixedCostType.HOSTING, 9L)).failure())
                .contains(FixedCostCommandFailure.EMPLOYEE_NOT_FOUND);
        verify(costs, never()).save(any());
    }

    @Test
    void updatesAnActiveCost() {
        FixedCost cost = stored();
        PaymentSchedule schedule = new PaymentSchedule(List.of(2));

        assertThat(service.handle(new UpdateFixedCostCommand(1L, "Updated", AMOUNT, schedule))
                .success()).contains(cost);
        assertThat(cost.getConcept()).isEqualTo("Updated");
        assertThat(cost.getMonthlyAmount()).isEqualTo(AMOUNT);
        assertThat(cost.getSchedule()).isEqualTo(schedule);
    }

    @Test
    void deactivatesIdempotently() {
        FixedCost cost = stored();

        assertThat(service.handle(new DeactivateFixedCostCommand(1L)).success()).contains(cost);
        assertThat(service.handle(new DeactivateFixedCostCommand(1L)).success()).contains(cost);
        assertThat(cost.isActive()).isFalse();
    }

    @Test
    void registersPaymentsWithAndWithoutABankAccountAndReturnsTheSavedInstance() {
        FixedCost cost = stored();
        FixedCost saved = mock(FixedCost.class);
        when(costs.save(cost)).thenReturn(saved);
        when(saved.getPayments()).thenAnswer(call -> cost.getPayments());
        when(saved.getType()).thenReturn(cost.getType());
        when(treasury.bankAccountExists(5L)).thenReturn(true);

        assertThat(service.handle(payment(null)).success()).contains(saved);
        verify(treasury, never()).bankAccountExists(any());
        assertThat(service.handle(payment(5L)).success()).contains(saved);
        assertThat(cost.getPayments()).hasSize(2);
        assertThat(cost.getPayments().get(1).getBankAccountId()).isEqualTo(5L);
    }

    @Test
    void rejectsChangesToAnUnknownCost() {
        assertThat(service.handle(new UpdateFixedCostCommand(1L, "Updated", AMOUNT, SCHEDULE)).failure())
                .contains(FixedCostCommandFailure.FIXED_COST_NOT_FOUND);
        assertThat(service.handle(new DeactivateFixedCostCommand(1L)).failure())
                .contains(FixedCostCommandFailure.FIXED_COST_NOT_FOUND);
        assertThat(service.handle(payment(null)).failure()).contains(FixedCostCommandFailure.FIXED_COST_NOT_FOUND);
        verify(costs, never()).save(any());
    }

    @Test
    void rejectsUpdatesAndPaymentsForAnInactiveCost() {
        FixedCost cost = stored();
        cost.deactivate();

        assertThat(service.handle(new UpdateFixedCostCommand(1L, "Updated", AMOUNT, SCHEDULE)).failure())
                .contains(FixedCostCommandFailure.FIXED_COST_INACTIVE);
        assertThat(service.handle(payment(9L)).failure()).contains(FixedCostCommandFailure.FIXED_COST_INACTIVE);
        assertThat(cost.getConcept()).isEqualTo("Monthly cost");
        assertThat(cost.getPayments()).isEmpty();
        verify(costs, never()).save(any());
        verify(treasury, never()).bankAccountExists(any());
    }

    @Test
    void rejectsAPaymentToAnUnknownBankAccount() {
        FixedCost cost = stored();

        assertThat(service.handle(payment(9L)).failure()).contains(FixedCostCommandFailure.BANK_ACCOUNT_NOT_FOUND);
        assertThat(cost.getPayments()).isEmpty();
        verify(costs, never()).save(any());
    }
}
