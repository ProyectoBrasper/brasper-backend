package com.brasper.platform.expenses.interfaces.acl;

import static org.assertj.core.api.Assertions.assertThat;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.expenses.domain.model.aggregates.Expense;
import com.brasper.platform.expenses.domain.model.commands.RegisterExpenseCommand;
import com.brasper.platform.expenses.domain.model.valueobjects.PaymentMethod;
import com.brasper.platform.expenses.domain.model.valueobjects.Voucher;
import com.brasper.platform.expenses.domain.model.valueobjects.VoucherType;
import com.brasper.platform.expenses.domain.repositories.ExpenseRepository;
import com.brasper.platform.shared.domain.model.valueobjects.CostCategory;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ExpensesContextFacadeTest {

    @Autowired
    private ExpensesContextFacade facade;
    @Autowired
    private ExpenseRepository expenses;

    private Expense expense(String day, Long projectId, CostCategory category, VoucherType voucher, String amount,
            PaymentMethod method, boolean paid) {
        Long account = method == PaymentMethod.PARTNER_CONTRIBUTION ? null : 9L;
        Long partner = method == PaymentMethod.PARTNER_CONTRIBUTION ? 7L : null;
        String number = voucher == VoucherType.NO_VOUCHER ? null : "X-1";
        return expenses.save(new Expense(new RegisterExpenseCommand(LocalDate.parse(day), projectId, null, category,
                null, new Voucher(voucher, number), new Money(new BigDecimal(amount), Currency.PEN), false, method,
                account, partner, paid), TaxRates.standard()));
    }

    /**
     * Project 3: invoice 1000 materials (IGV 180) Sep 1 paid by bank; receipt 300 labor Sep 2 paid from petty
     * cash; invoice 500 materials Sep 2 unpaid; a 2000 invoice annulled; 400 receipt paid by a partner. Plus a
     * general 100 invoice on Sep 3 (project null) paid by bank. Sep 1 2026 is a Tuesday: week 1 is Sep 1-6.
     */
    private void seed() {
        expense("2026-09-01", 3L, CostCategory.MATERIALS, VoucherType.INVOICE, "1000", PaymentMethod.BANK_ACCOUNT, true);
        expense("2026-09-02", 3L, CostCategory.LABOR, VoucherType.SALES_RECEIPT, "300", PaymentMethod.PETTY_CASH, true);
        expense("2026-09-02", 3L, CostCategory.MATERIALS, VoucherType.INVOICE, "500", PaymentMethod.BANK_ACCOUNT, false);
        expense("2026-09-02", 3L, CostCategory.OTHER, VoucherType.INVOICE, "2000", PaymentMethod.BANK_ACCOUNT, true)
                .annul();
        expense("2026-09-03", 3L, CostCategory.OTHER, VoucherType.SALES_RECEIPT, "400",
                PaymentMethod.PARTNER_CONTRIBUTION, true);
        expense("2026-09-03", null, CostCategory.OTHER, VoucherType.INVOICE, "100", PaymentMethod.BANK_ACCOUNT, true);
    }

    @Test
    void directCostExcludesIgvAnnulledAndGeneralExpenses() {
        seed();

        assertThat(facade.sumDirectCostByProject(3L)).isEqualByComparingTo("2200");
        assertThat(facade.fetchDirectCostByCategory(3L)).hasSize(3)
                .containsEntry("MATERIALS", new BigDecimal("1500.00")).containsEntry("LABOR", new BigDecimal("300.00"))
                .containsEntry("OTHER", new BigDecimal("400.00"));
        assertThat(facade.sumDirectCostByProject(99L)).isEqualByComparingTo("0");
        assertThat(facade.fetchDirectCostByCategory(99L)).isEmpty();
    }

    @Test
    void inputIgvCountsOnlyActiveInvoicesOfTheMonth() {
        seed();

        // 180 + 90 + 18 (the 100 general invoice); the annulled 2000 invoice and the receipts give none.
        assertThat(facade.sumInputIgvByPeriod(2026, 9)).isEqualByComparingTo("288");
        assertThat(facade.sumInputIgvByPeriod(2026, 8)).isEqualByComparingTo("0");
    }

    @Test
    void paidByWeekCountsPaidBankAndPettyCashExpensesOfThatWeek() {
        seed();

        // 1180 (bank invoice) + 300 (petty cash) + 118 (general bank invoice); unpaid, annulled and partner out.
        assertThat(facade.sumPaidByWeek(2026, 9, 1)).isEqualByComparingTo("1598");
        assertThat(facade.sumPaidByWeek(2026, 9, 2)).isEqualByComparingTo("0");
    }
}
