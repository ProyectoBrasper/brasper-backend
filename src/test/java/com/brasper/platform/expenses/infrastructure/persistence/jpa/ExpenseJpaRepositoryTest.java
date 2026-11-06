package com.brasper.platform.expenses.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.expenses.domain.model.aggregates.Expense;
import com.brasper.platform.expenses.domain.model.commands.CorrectExpenseCommand;
import com.brasper.platform.expenses.domain.model.commands.RegisterExpenseCommand;
import com.brasper.platform.expenses.domain.model.valueobjects.PaymentMethod;
import com.brasper.platform.expenses.domain.model.valueobjects.Voucher;
import com.brasper.platform.expenses.domain.model.valueobjects.VoucherType;
import com.brasper.platform.expenses.domain.repositories.ExpenseRepository;
import com.brasper.platform.shared.domain.model.valueobjects.CostCategory;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.PaymentStatus;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ExpenseJpaRepositoryTest {

    @Autowired
    private ExpenseRepository expenses;
    @Autowired
    private EntityManager em;

    private static Expense invoiceExpense(Long projectId) {
        return new Expense(new RegisterExpenseCommand(LocalDate.of(2026, 8, 20), projectId, 5L,
                CostCategory.MATERIALS, "cables", new Voucher(VoucherType.INVOICE, "F001-123"),
                new Money(new BigDecimal("1000"), Currency.USD), false, PaymentMethod.BANK_ACCOUNT, 9L, null, false),
                TaxRates.standard());
    }

    @Test
    void persistsEveryFieldAndTheVoucherAndAmountsRoundTrip() {
        Long id = expenses.save(invoiceExpense(3L)).getId();
        em.flush();
        em.clear();

        Expense found = expenses.findById(id).orElseThrow();

        assertThat(found.getSpentOn()).isEqualTo(LocalDate.of(2026, 8, 20));
        assertThat(found.getProjectId()).isEqualTo(3L);
        assertThat(found.getSupplierId()).isEqualTo(5L);
        assertThat(found.getCategory()).isEqualTo(CostCategory.MATERIALS);
        assertThat(found.getVoucher()).isEqualTo(new Voucher(VoucherType.INVOICE, "F001-123"));
        assertThat(found.getAmounts().total()).isEqualTo(new Money(new BigDecimal("1180.00"), Currency.USD));
        assertThat(found.getAmounts().igv().amount()).isEqualByComparingTo("180");
        assertThat(found.getPaymentMethod()).isEqualTo(PaymentMethod.BANK_ACCOUNT);
        assertThat(found.getBankAccountId()).isEqualTo(9L);
        assertThat(found.getPartnerId()).isNull();
        assertThat(found.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(found.isAnnulled()).isFalse();
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void correctionsAnnulmentAndPaymentArePersisted() {
        Long id = expenses.save(invoiceExpense(3L)).getId();
        em.flush();
        em.clear();

        Expense expense = expenses.findById(id).orElseThrow();
        expense.correct(new CorrectExpenseCommand(id, null, CostCategory.OTHER, "  "));
        expense.markAsPaid();
        expenses.save(expense);
        em.flush();
        em.clear();

        Expense corrected = expenses.findById(id).orElseThrow();
        assertThat(corrected.getProjectId()).isNull();
        assertThat(corrected.getCategory()).isEqualTo(CostCategory.OTHER);
        assertThat(corrected.getDescription()).isNull();
        assertThat(corrected.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);

        corrected.annul();
        expenses.save(corrected);
        em.flush();
        em.clear();
        assertThat(expenses.findById(id).orElseThrow().isAnnulled()).isTrue();
        assertThat(expenses.findAll()).hasSize(1);
    }

    @Test
    void theDatabaseRejectsAnExpenseWithoutItsPaymentTarget() {
        assertThatThrownBy(() -> em.createNativeQuery("""
                insert into expenses (spent_on, category, voucher_type, currency, taxable_base, igv, total,
                    payment_method, payment_status, created_at, updated_at)
                values ('2026-08-20', 'OTHER', 'NO_VOUCHER', 'PEN', 10, 0, 10, 'BANK_ACCOUNT', 'PAID', now(), now())
                """).executeUpdate()).hasMessageContaining("ck_expenses_payment_target");
    }
}
