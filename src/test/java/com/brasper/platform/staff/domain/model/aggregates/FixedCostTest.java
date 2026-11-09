package com.brasper.platform.staff.domain.model.aggregates;

import static org.assertj.core.api.Assertions.*;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import com.brasper.platform.staff.domain.model.commands.*;
import com.brasper.platform.staff.domain.model.valueobjects.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class FixedCostTest {
    private static final Period PERIOD = new Period(2026, 11);
    private static Money money(String amount) { return new Money(new BigDecimal(amount), Currency.PEN); }
    private static FixedCost cost(FixedCostType type, Long employee) {
        return new FixedCost(new CreateFixedCostCommand(new CostCode(" BR01 "), " salary ", type,
                money("100"), employee, new PaymentSchedule(List.of(5))));
    }
    private static RegisterFixedCostPaymentCommand payment(Period period, Money amount) {
        return new RegisterFixedCostPaymentCommand(null, LocalDate.of(2026, 11, 30), period, amount, null, "paid");
    }
    @Test void validatesCodeSalaryAndSchedule() {
        assertThatThrownBy(() -> cost(FixedCostType.SALARY, null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(cost(FixedCostType.SALARY, 1L).getEmployeeId()).isEqualTo(1L);
        for (String code : List.of(" ", "x".repeat(31)))
            assertThatThrownBy(() -> new CostCode(code)).isInstanceOf(IllegalArgumentException.class);
        for (List<Integer> weeks : List.of(List.of(0), List.of(6), List.of(1, 1)))
            assertThatThrownBy(() -> new PaymentSchedule(weeks)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void tracksPaymentsPerPeriodAndClampsOverpayment() {
        FixedCost cost = cost(FixedCostType.HOSTING, null);
        assertThat(cost.paidIn(PERIOD)).isEqualTo(money("0"));
        assertThat(cost.pendingIn(PERIOD)).isEqualTo(money("100"));
        var first = cost.registerPayment(payment(PERIOD, money("40")));
        assertThat(first).extracting("paidOn", "period", "amount", "bankAccountId", "notes")
                .containsExactly(LocalDate.of(2026, 11, 30), PERIOD, money("40"), null, "paid");
        cost.registerPayment(payment(PERIOD.next(), money("10")));
        assertThat(cost.paidIn(PERIOD)).isEqualTo(money("40"));
        assertThat(cost.pendingIn(PERIOD)).isEqualTo(money("60"));
        cost.registerPayment(payment(PERIOD, money("60")));
        assertThat(cost.pendingIn(PERIOD)).isEqualTo(money("0"));
        cost.registerPayment(payment(PERIOD, money("1")));
        assertThat(cost.pendingIn(PERIOD)).isEqualTo(money("0"));
    }
    @Test void validatesUpdatesAndPaymentsAndDeactivates() {
        FixedCost cost = cost(FixedCostType.HOSTING, null);
        for (String amount : List.of("0", "-1", "1000000000000")) {
            assertThatThrownBy(() -> cost.registerPayment(payment(PERIOD, money(amount)))).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> cost.update(new UpdateFixedCostCommand(null, "new", money(amount), new PaymentSchedule(List.of()))))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        Money usd = new Money(BigDecimal.ONE, Currency.USD);
        assertThatThrownBy(() -> cost.registerPayment(payment(PERIOD, usd))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cost.update(new UpdateFixedCostCommand(null, "new", usd, new PaymentSchedule(List.of()))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(cost.getPayments()).isEmpty();
        cost.update(new UpdateFixedCostCommand(null, " rent ", money("200"), new PaymentSchedule(List.of(1, 3))));
        assertThat(cost.getConcept()).isEqualTo("rent");
        assertThat(cost.getMonthlyAmount()).isEqualTo(money("200"));
        assertThat(cost.getSchedule().expectedWeeks()).containsExactly(1, 3);
        cost.deactivate(); assertThat(cost.isActive()).isFalse();
        assertThatThrownBy(() -> cost.registerPayment(payment(PERIOD, money("1")))).isInstanceOf(IllegalStateException.class);
    }
}
