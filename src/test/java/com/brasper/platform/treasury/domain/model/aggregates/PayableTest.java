package com.brasper.platform.treasury.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.PaymentStatus;
import com.brasper.platform.treasury.domain.model.commands.CreatePayableCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterPayablePaymentCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.PayableType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PayableTest {
    private static Money money(String amount) { return new Money(new BigDecimal(amount), Currency.PEN); }
    private static Payable payable(String concept, String amount) {
        return new Payable(new CreatePayableCommand(concept, PayableType.SUPPLIER, money(amount),
                null, null, null, new CashFlowWeek(2026, 11, 6)));
    }
    private static RegisterPayablePaymentCommand payment(Money amount) {
        return new RegisterPayablePaymentCommand(null, LocalDate.of(2026, 11, 30), amount, 1L);
    }
    @Test
    void partialAndExactPaymentsSettleTheBalanceAndKeepChildrenReadOnly() {
        Payable payable = payable(" materials ", "100");
        var first = payable.registerPayment(payment(money("40")));
        assertThat(payable.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
        assertThat(payable.balance()).isEqualTo(money("60"));
        assertThatThrownBy(() -> payable.registerPayment(payment(money("60.01"))))
                .isInstanceOf(IllegalStateException.class);
        assertThat(payable.getPayments()).containsExactly(first);
        payable.registerPayment(payment(money("60")));
        assertThat(payable.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payable.balance()).isEqualTo(money("0"));
        assertThatThrownBy(() -> payable.registerPayment(payment(money("1"))))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> payable.getPayments().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
    @Test
    void rejectsInvalidDetailsAndPaymentsWithoutChangingThePayable() {
        Payable payable = payable("materials", "100");
        assertThatThrownBy(() -> payable("materials", "999999999999.999")).isInstanceOf(IllegalArgumentException.class);
        for (String amount : new String[]{"0", "-1", "0.004"}) {
            assertThatThrownBy(() -> payable("materials", amount)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> payable.registerPayment(payment(money(amount)))).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> payable.registerPayment(payment(new Money(BigDecimal.ONE, Currency.USD))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(payable.getPayments()).isEmpty();
        assertThat(payable.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }
}
