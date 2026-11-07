package com.brasper.platform.staff.application.queryservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import com.brasper.platform.staff.domain.model.aggregates.FixedCost;
import com.brasper.platform.staff.domain.model.commands.CreateFixedCostCommand;
import com.brasper.platform.staff.domain.model.commands.RegisterFixedCostPaymentCommand;
import com.brasper.platform.staff.domain.model.valueobjects.CostCode;
import com.brasper.platform.staff.domain.model.valueobjects.FixedCostType;
import com.brasper.platform.staff.domain.model.valueobjects.PaymentSchedule;
import com.brasper.platform.staff.domain.repositories.FixedCostRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class FixedCostFiguresTest {

    private static final Period MARCH = new Period(2038, 3);
    private static final CashFlowWeek WEEK_1 = new CashFlowWeek(2038, 3, 1);
    private static final CashFlowWeek WEEK_2 = new CashFlowWeek(2038, 3, 2);
    private static final CashFlowWeek WEEK_3 = new CashFlowWeek(2038, 3, 3);

    private final FixedCostRepository costs = mock(FixedCostRepository.class);
    private final FixedCostFigures figures = new FixedCostFigures(costs);

    private static Money pen(String amount) {
        return new Money(new BigDecimal(amount), Currency.PEN);
    }

    private static FixedCost cost(FixedCostType type, String amount, Integer... weeks) {
        return new FixedCost(new CreateFixedCostCommand(new CostCode("FC-" + type), "Cost", type, pen(amount),
                type == FixedCostType.SALARY ? 1L : null, new PaymentSchedule(List.of(weeks))));
    }

    private static void pay(FixedCost cost, LocalDate on, String amount) {
        cost.registerPayment(new RegisterFixedCostPaymentCommand(null, on, MARCH, pen(amount), null, null));
    }

    @Test
    void splitsTheMonthlyAmountAcrossTheExpectedWeeksUntilTheMonthIsPaid() {
        FixedCost salary = cost(FixedCostType.SALARY, "1000", 1, 3);
        FixedCost rent = cost(FixedCostType.OFFICE_RENT, "700", 2);
        when(costs.findAllByActive(true)).thenReturn(List.of(salary, rent));

        assertThat(figures.scheduledIn(WEEK_1)).containsOnlyKeys("SALARY").containsEntry("SALARY", new BigDecimal("500.00"));
        assertThat(figures.scheduledIn(WEEK_2)).containsOnlyKeys("OFFICE_RENT");

        pay(salary, LocalDate.of(2038, 3, 2), "1000");

        assertThat(figures.scheduledIn(WEEK_3)).isEmpty();
    }

    @Test
    void sumsThePaymentsMadeDuringTheWeekByType() {
        FixedCost salary = cost(FixedCostType.SALARY, "1000", 1, 3);
        pay(salary, LocalDate.of(2038, 3, 3), "400");
        pay(salary, LocalDate.of(2038, 3, 16), "600");
        when(costs.findAll()).thenReturn(List.of(salary));

        assertThat(figures.paidIn(WEEK_1)).containsOnlyKeys("SALARY").containsEntry("SALARY", new BigDecimal("400.00"));
        assertThat(figures.paidIn(WEEK_2)).isEmpty();
        assertThat(figures.paidIn(WEEK_3)).containsEntry("SALARY", new BigDecimal("600.00"));
    }
}
