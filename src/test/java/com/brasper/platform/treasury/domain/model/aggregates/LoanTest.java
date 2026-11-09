package com.brasper.platform.treasury.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.PaymentStatus;
import com.brasper.platform.shared.domain.model.valueobjects.Percentage;
import com.brasper.platform.treasury.domain.model.commands.RegisterLoanCommand;
import com.brasper.platform.treasury.domain.model.commands.ScheduleLoanInstallmentCommand;
import com.brasper.platform.treasury.domain.model.commands.PayLoanInstallmentCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.LoanType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LoanTest {
    private static final LocalDate DATE = LocalDate.of(2026, 11, 30);
    private static Money money(String amount) { return new Money(new BigDecimal(amount), Currency.PEN); }
    private static Loan loan(String party, String principal) {
        return new Loan(new RegisterLoanCommand(LoanType.RECEIVED, party, money(principal), new Percentage(BigDecimal.ZERO), DATE, 1L));
    }
    private static ScheduleLoanInstallmentCommand schedule(Money principal, Money interest) {
        return new ScheduleLoanInstallmentCommand(null, DATE, principal, interest);
    }
    @Test
    void validatesRegistration() {
        for (String party : new String[]{null, " ", "x".repeat(201)})
            assertThatThrownBy(() -> loan(party, "100")).isInstanceOf(IllegalArgumentException.class);
        for (String amount : new String[]{"0", "-1", "999999999999.999"})
            assertThatThrownBy(() -> loan("lender", amount)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test
    void validatesScheduleAndLeavesTheLoanUnchangedOnFailure() {
        Loan loan = loan("lender", "100");
        loan.scheduleInstallment(schedule(money("40"), money("5")));
        assertThatThrownBy(() -> loan.scheduleInstallment(schedule(money("60.01"), money("0")))).isInstanceOf(IllegalStateException.class);
        for (Money principal : new Money[]{money("0"), money("-1"), new Money(BigDecimal.ONE, Currency.USD)})
            assertThatThrownBy(() -> loan.scheduleInstallment(schedule(principal, money("0")))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> loan.scheduleInstallment(schedule(money("1"), money("-1")))).isInstanceOf(IllegalArgumentException.class);
        assertThat(loan).extracting(Loan::totalInterest, Loan::outstandingBalance, Loan::getStatus)
                .containsExactly(money("5"), money("100"), PaymentStatus.PENDING);
        assertThatThrownBy(() -> loan.getInstallments().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
    @Test
    void payingAllScheduledPrincipalDoesNotSettleAnUnderScheduledLoan() throws Exception {
        Loan loan = loan("lender", "100");
        var first = loan.scheduleInstallment(schedule(money("40"), money("5")));
        var id = first.getClass().getDeclaredField("id"); id.setAccessible(true); id.set(first, 1L);
        var payment = new PayLoanInstallmentCommand(null, 1L, DATE, 1L);
        assertThatThrownBy(() -> loan.payInstallment(new PayLoanInstallmentCommand(null, 99L, DATE, 1L))).isInstanceOf(IllegalArgumentException.class);
        loan.payInstallment(payment);
        assertThat(first.total()).isEqualTo(money("45"));
        assertThat(loan.outstandingBalance()).isEqualTo(money("60"));
        assertThat(loan.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
        assertThatThrownBy(() -> loan.payInstallment(payment)).isInstanceOf(IllegalStateException.class);
        var second = loan.scheduleInstallment(schedule(money("60"), money("3"))); id.set(second, 2L);
        loan.payInstallment(new PayLoanInstallmentCommand(null, 2L, DATE, 1L));
        assertThat(loan).extracting(Loan::getStatus, Loan::outstandingBalance, Loan::totalInterest)
                .containsExactly(PaymentStatus.PAID, money("0"), money("8"));
    }
}
