package com.brasper.platform.treasury.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.accounting.application.queryservices.LedgerQueryService;
import com.brasper.platform.accounting.domain.model.valueobjects.EntrySource;
import com.brasper.platform.expenses.application.commandservices.ExpenseCommandService;
import com.brasper.platform.expenses.application.queryservices.ExpenseQueryService;
import com.brasper.platform.expenses.domain.model.commands.AnnulExpenseCommand;
import com.brasper.platform.expenses.domain.model.commands.RegisterExpenseCommand;
import com.brasper.platform.expenses.domain.model.valueobjects.PaymentMethod;
import com.brasper.platform.expenses.domain.model.valueobjects.Voucher;
import com.brasper.platform.expenses.domain.model.valueobjects.VoucherType;
import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.CostCategory;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.PaymentStatus;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import com.brasper.platform.treasury.application.commandservices.BankAccountCommandService;
import com.brasper.platform.treasury.application.commandservices.PayableCommandService;
import com.brasper.platform.treasury.application.queryservices.PayableQueryService;
import com.brasper.platform.treasury.domain.model.aggregates.Payable;
import com.brasper.platform.treasury.domain.model.commands.CreatePayableCommand;
import com.brasper.platform.treasury.domain.model.commands.OpenBankAccountCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterPayablePaymentCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountNumber;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountType;
import com.brasper.platform.treasury.domain.model.valueobjects.Bank;
import com.brasper.platform.treasury.domain.model.valueobjects.PayableType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

/** Not @Transactional on purpose: the handlers run when the operation commits. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ExpensePayablesFlowTest {

    private static final LocalDate DAY = LocalDate.of(2036, 2, 12);

    @Autowired private ExpenseCommandService expenses;
    @Autowired private ExpenseQueryService expenseQueries;
    @Autowired private PayableCommandService payables;
    @Autowired private PayableQueryService payableQueries;
    @Autowired private BankAccountCommandService bankAccounts;
    @Autowired private LedgerQueryService ledger;
    @Autowired private TransactionTemplate transaction;

    private static Money pen(String amount) {
        return new Money(new BigDecimal(amount), Currency.PEN);
    }

    private Long bank(String number) {
        return bankAccounts.handle(new OpenBankAccountCommand(Bank.BCP, new AccountNumber(number),
                AccountType.CHECKING, Currency.PEN, pen("5000"), LocalDate.of(2036, 1, 1))).success().orElseThrow().getId();
    }

    private Long pendingExpense(String number, Long bank) {
        return expenses.handle(new RegisterExpenseCommand(DAY, null, null, CostCategory.OTHER, "Pending service",
                new Voucher(VoucherType.INVOICE, number), pen("1000"), false, PaymentMethod.BANK_ACCOUNT, bank, null, false))
                .success().orElseThrow().getId();
    }

    private Payable payableOf(Long expenseId) {
        return payableQueries.findAll().stream().filter(p -> expenseId.equals(p.getExpenseId())).findFirst().orElse(null);
    }

    private List<String> accountsOf(EntrySource source, Long sourceId, Period period) {
        return transaction.execute(status -> ledger.findEntriesByPeriod(period).stream()
                .filter(e -> e.getSource() == source && sourceId.equals(e.getSourceId()))
                .flatMap(e -> ledger.findEntryById(e.getId()).orElseThrow().getLines().stream())
                .map(l -> l.getAccountCode().value() + (l.getDebit().isPositive() ? " D " : " H ")
                        + (l.getDebit().isPositive() ? l.getDebit() : l.getCredit()).amount().intValue()).toList());
    }

    @Test
    void aPendingExpenseBecomesAPayableAndPayingItSettlesTheExpense() {
        Long bank = bank("2036-1");
        Long expenseId = pendingExpense("F036-1", bank);
        Payable payable = payableOf(expenseId);
        assertThat(payable).isNotNull();
        assertThat(payable.getTotalAmount().amount()).isEqualByComparingTo("1180");
        assertThat(payable.getScheduledWeek()).isEqualTo(CashFlowWeek.from(DAY));
        assertThat(accountsOf(EntrySource.EXPENSE, expenseId, new Period(2036, 2)))
                .containsExactly("63 D 1000", "40111 D 180", "42 H 1180");

        payables.handle(new RegisterPayablePaymentCommand(payable.getId(), DAY.plusDays(5), pen("500"), bank))
                .success().orElseThrow();
        assertThat(expenseQueries.findById(expenseId).orElseThrow().getPaymentStatus()).isNotEqualTo(PaymentStatus.PAID);

        payables.handle(new RegisterPayablePaymentCommand(payable.getId(), DAY.plusDays(6), pen("680"), bank))
                .success().orElseThrow();
        assertThat(expenseQueries.findById(expenseId).orElseThrow().getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void annullingAPendingExpenseRemovesItsPayable() {
        Long expenseId = pendingExpense("F036-2", bank("2036-2"));
        assertThat(payableOf(expenseId)).isNotNull();

        expenses.handle(new AnnulExpenseCommand(expenseId)).success().orElseThrow();

        assertThat(payableOf(expenseId)).isNull();
    }

    @Test
    void aPayableThatIsNotAnExpenseBooksTheCostAndTheDebtWhenItIsCreated() {
        Payable payable = payables.handle(new CreatePayableCommand("Contractor", PayableType.SUPPLIER, pen("700"), null,
                null, null, CashFlowWeek.from(DAY))).success().orElseThrow();

        Period today = new Period(LocalDate.now().getYear(), LocalDate.now().getMonthValue());
        assertThat(accountsOf(EntrySource.PAYABLE_REGISTERED, payable.getId(), today))
                .containsExactly("63 D 700", "42 H 700");
    }
}
