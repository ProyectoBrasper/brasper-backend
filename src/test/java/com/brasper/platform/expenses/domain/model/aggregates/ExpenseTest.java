package com.brasper.platform.expenses.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.expenses.domain.model.commands.CorrectExpenseCommand;
import com.brasper.platform.expenses.domain.model.commands.RegisterExpenseCommand;
import com.brasper.platform.expenses.domain.model.valueobjects.PaymentMethod;
import com.brasper.platform.expenses.domain.model.valueobjects.Voucher;
import com.brasper.platform.expenses.domain.model.valueobjects.VoucherType;
import com.brasper.platform.shared.domain.model.valueobjects.CostCategory;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.PaymentStatus;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ExpenseTest {

    private static final Voucher INVOICE = new Voucher(VoucherType.INVOICE, "F001-123");
    private static final Voucher RECEIPT = new Voucher(VoucherType.SALES_RECEIPT, "B001-9");

    private static Expense expense(Voucher voucher, String amount, boolean includesIgv, PaymentMethod method,
            Long account, Long partner, boolean paid, Long projectId) {
        return new Expense(new RegisterExpenseCommand(LocalDate.of(2026, 8, 20), projectId, 5L,
                CostCategory.MATERIALS, " cables ", voucher, new Money(new BigDecimal(amount), Currency.PEN),
                includesIgv, method, account, partner, paid), TaxRates.standard());
    }

    private static Expense bankExpense(Voucher voucher, String amount, boolean includesIgv) {
        return expense(voucher, amount, includesIgv, PaymentMethod.BANK_ACCOUNT, 9L, null, true, 3L);
    }

    @Test
    void anInvoiceAddsIgvToAnAmountWithoutIt() {
        Expense expense = bankExpense(INVOICE, "1000", false);

        assertThat(expense.getAmounts().taxableBase().amount()).isEqualByComparingTo("1000");
        assertThat(expense.getAmounts().igv().amount()).isEqualByComparingTo("180");
        assertThat(expense.getAmounts().total().amount()).isEqualByComparingTo("1180");
        assertThat(expense.generatesTaxCredit()).isTrue();
    }

    @Test
    void anInvoiceSplitsIgvOutOfAnAmountThatIncludesIt() {
        Expense expense = bankExpense(INVOICE, "1180", true);

        assertThat(expense.getAmounts().taxableBase().amount()).isEqualByComparingTo("1000");
        assertThat(expense.getAmounts().igv().amount()).isEqualByComparingTo("180");
        assertThat(expense.getAmounts().total().amount()).isEqualByComparingTo("1180");
    }

    @Test
    void anyOtherVoucherIsBookedWithoutIgv() {
        for (Voucher voucher : new Voucher[] {RECEIPT, new Voucher(VoucherType.FEE_RECEIPT, "E001-1"),
                new Voucher(VoucherType.NO_VOUCHER, null)}) {
            Expense expense = bankExpense(voucher, "500", true);

            assertThat(expense.getAmounts().taxableBase().amount()).isEqualByComparingTo("500");
            assertThat(expense.getAmounts().igv().amount()).isEqualByComparingTo("0");
            assertThat(expense.getAmounts().total().amount()).isEqualByComparingTo("500");
            assertThat(expense.generatesTaxCredit()).isFalse();
        }
    }

    @Test
    void keepsTheDataAndStartsActive() {
        Expense expense = bankExpense(INVOICE, "1000", false);

        assertThat(expense.getProjectId()).isEqualTo(3L);
        assertThat(expense.getDescription()).isEqualTo("cables");
        assertThat(expense.getVoucher()).isEqualTo(INVOICE);
        assertThat(expense.isAnnulled()).isFalse();
        assertThat(expense.isDirectCost()).isTrue();
    }

    @Test
    void anExpenseWithoutProjectIsGeneralAndAPaidFlagSetsTheStatus() {
        Expense general = expense(RECEIPT, "50", false, PaymentMethod.PETTY_CASH, 2L, null, false, null);

        assertThat(general.isDirectCost()).isFalse();
        assertThat(general.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(bankExpense(INVOICE, "10", false).getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void paymentMethodNeedsItsOwnTargetAndOnlyThat() {
        Expense partner = expense(RECEIPT, "50", false, PaymentMethod.PARTNER_CONTRIBUTION, null, 7L, true, 3L);
        assertThat(partner.getPartnerId()).isEqualTo(7L);
        assertThat(partner.getBankAccountId()).isNull();

        assertThatThrownBy(() -> expense(RECEIPT, "50", false, PaymentMethod.BANK_ACCOUNT, null, null, true, 3L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> expense(RECEIPT, "50", false, PaymentMethod.PETTY_CASH, null, null, true, 3L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> expense(RECEIPT, "50", false, PaymentMethod.PARTNER_CONTRIBUTION, null, null, true, 3L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> expense(RECEIPT, "50", false, PaymentMethod.BANK_ACCOUNT, 9L, 7L, true, 3L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> expense(RECEIPT, "50", false, PaymentMethod.PARTNER_CONTRIBUTION, 9L, 7L, true, 3L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAZeroAmountAndMissingRequiredFields() {
        assertThatThrownBy(() -> bankExpense(INVOICE, "0", false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Expense(new RegisterExpenseCommand(null, 3L, 5L, CostCategory.LABOR, null,
                INVOICE, new Money(BigDecimal.TEN, Currency.PEN), false, PaymentMethod.BANK_ACCOUNT, 9L, null, true),
                TaxRates.standard())).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Expense(new RegisterExpenseCommand(LocalDate.of(2026, 8, 20), 3L, 5L, null, null,
                INVOICE, new Money(BigDecimal.TEN, Currency.PEN), false, PaymentMethod.BANK_ACCOUNT, 9L, null, true),
                TaxRates.standard())).isInstanceOf(NullPointerException.class);
    }

    @Test
    void correctReclassifiesButKeepsAmountsAndPayment() {
        Expense expense = bankExpense(INVOICE, "1000", false);

        expense.correct(new CorrectExpenseCommand(1L, null, CostCategory.TRAVEL_EXPENSES, " taxi "));

        assertThat(expense.getProjectId()).isNull();
        assertThat(expense.isDirectCost()).isFalse();
        assertThat(expense.getCategory()).isEqualTo(CostCategory.TRAVEL_EXPENSES);
        assertThat(expense.getDescription()).isEqualTo("taxi");
        assertThat(expense.getAmounts().total().amount()).isEqualByComparingTo("1180");
        assertThatThrownBy(() -> expense.correct(new CorrectExpenseCommand(1L, 3L, null, null)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void markAsPaidSettlesAPendingExpense() {
        Expense expense = expense(RECEIPT, "50", false, PaymentMethod.PETTY_CASH, 2L, null, false, 3L);

        expense.markAsPaid();

        assertThat(expense.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void anAnnulledExpenseCannotBeAnnulledCorrectedOrPaidAgain() {
        Expense expense = bankExpense(INVOICE, "1000", false);

        expense.annul();

        assertThat(expense.isAnnulled()).isTrue();
        assertThatThrownBy(expense::annul).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(expense::markAsPaid).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> expense.correct(new CorrectExpenseCommand(1L, 3L, CostCategory.OTHER, null)))
                .isInstanceOf(IllegalStateException.class);
    }
}
