package com.brasper.platform.accounting.domain.model.aggregates;

import static org.assertj.core.api.Assertions.*;
import com.brasper.platform.accounting.domain.model.commands.PrepareTaxReturnCommand;
import com.brasper.platform.accounting.domain.model.valueobjects.TaxReturnStatus;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TaxReturnTest {
    private static final PrepareTaxReturnCommand COMMAND = new PrepareTaxReturnCommand(new Period(2026, 11));
    private static final LocalDate DATE = LocalDate.of(2026, 12, 1);
    private Money money(int amount) { return new Money(BigDecimal.valueOf(amount), Currency.PEN); }
    private TaxReturn prepare(int output, int input, int credit) {
        return new TaxReturn(COMMAND, money(output), money(input), money(credit), money(15));
    }
    @Test void calculatesPayableAndCarriedCredit() {
        TaxReturn payable = prepare(100, 30, 20);
        assertThat(payable.igvPayable()).isEqualTo(money(50));
        assertThat(payable.totalPayable()).isEqualTo(money(65));
        assertThat(payable.nextCarriedCredit()).isEqualTo(money(0));
        TaxReturn credit = prepare(30, 40, 20);
        assertThat(credit.igvPayable()).isEqualTo(money(0));
        assertThat(credit.nextCarriedCredit()).isEqualTo(money(30));
        assertThat(credit.totalPayable()).isEqualTo(money(15));
        assertThat(prepare(50, 30, 20).igvPayable()).isEqualTo(money(0));
        assertThat(prepare(50, 30, 20).nextCarriedCredit()).isEqualTo(money(0));
    }
    @Test void rejectsNegativeAmountsAndMixedCurrencies() {
        for (int i = 0; i < 4; i++) {
            Money[] amounts = {money(1), money(1), money(1), money(1)};
            amounts[i] = money(-1);
            assertThatThrownBy(() -> new TaxReturn(COMMAND, amounts[0], amounts[1], amounts[2], amounts[3]))
                    .isInstanceOf(IllegalArgumentException.class);
            amounts[i] = new Money(BigDecimal.ONE, Currency.USD);
            assertThatThrownBy(() -> new TaxReturn(COMMAND, amounts[0], amounts[1], amounts[2], amounts[3]))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
    @Test void enforcesFilingAndPaymentTransitions() {
        TaxReturn tax = prepare(100, 30, 20);
        assertThat(tax.getStatus()).isEqualTo(TaxReturnStatus.DRAFT);
        assertThatThrownBy(() -> tax.markAsPaid(DATE, 1L)).isInstanceOf(IllegalStateException.class);
        tax.file();
        assertThat(tax.getStatus()).isEqualTo(TaxReturnStatus.FILED);
        assertThatThrownBy(tax::file).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> tax.markAsPaid(null, 1L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tax.markAsPaid(DATE, null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(tax.getStatus()).isEqualTo(TaxReturnStatus.FILED);
        tax.markAsPaid(DATE, 1L);
        assertThat(tax.getStatus()).isEqualTo(TaxReturnStatus.PAID);
        assertThat(tax.getPaidOn()).isEqualTo(DATE);
        assertThat(tax.getBankAccountId()).isEqualTo(1L);
        assertThatThrownBy(tax::file).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> tax.markAsPaid(DATE, 1L)).isInstanceOf(IllegalStateException.class);
    }
}
