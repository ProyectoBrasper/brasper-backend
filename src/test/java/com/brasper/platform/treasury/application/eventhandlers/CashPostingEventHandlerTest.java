package com.brasper.platform.treasury.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.accounting.domain.events.TaxReturnPaidEvent;
import com.brasper.platform.billing.domain.events.InvoicePaymentReceivedEvent;
import com.brasper.platform.billing.domain.model.valueobjects.InvoicePaymentType;
import com.brasper.platform.expenses.domain.events.ExpenseAnnulledEvent;
import com.brasper.platform.expenses.domain.events.ExpenseRegisteredEvent;
import com.brasper.platform.expenses.domain.model.valueobjects.PaymentMethod;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.staff.domain.events.FixedCostPaidEvent;
import com.brasper.platform.staff.domain.model.valueobjects.FixedCostType;
import com.brasper.platform.treasury.application.commandservices.BankAccountCommandService;
import com.brasper.platform.treasury.application.queryservices.BankAccountQueryService;
import com.brasper.platform.treasury.domain.model.commands.OpenBankAccountCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountNumber;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountType;
import com.brasper.platform.treasury.domain.model.valueobjects.Bank;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

/** Not @Transactional on purpose: the handlers run when the publishing transaction commits. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CashPostingEventHandlerTest {

    private static final LocalDate DAY = LocalDate.of(2035, 7, 10);

    @Autowired private ApplicationEventPublisher publisher;
    @Autowired private TransactionTemplate transaction;
    @Autowired private BankAccountCommandService accounts;
    @Autowired private BankAccountQueryService queries;

    private static Money pen(String amount) {
        return new Money(new BigDecimal(amount), Currency.PEN);
    }

    private Long bankAccount(String number) {
        return accounts.handle(new OpenBankAccountCommand(Bank.BCP, new AccountNumber(number), AccountType.CHECKING,
                Currency.PEN, pen("1000"), DAY)).success().orElseThrow().getId();
    }

    private BigDecimal balance(Long id) {
        return queries.findById(id).orElseThrow().currentBalance().amount();
    }

    private void publish(Object event) {
        transaction.executeWithoutResult(status -> publisher.publishEvent(event));
    }

    @Test
    void depositsInvoicePaymentsAndChargesPaidExpensesAndGivesTheMoneyBackWhenAnnulled() {
        Long bank = bankAccount("993-1");

        publish(new InvoicePaymentReceivedEvent(7001L, 1L, DAY, pen("500"), InvoicePaymentType.NET_DEPOSIT, bank));
        assertThat(balance(bank)).isEqualByComparingTo("1500");

        publish(new InvoicePaymentReceivedEvent(7002L, 1L, DAY, pen("35"), InvoicePaymentType.RETENTION_CERTIFICATE, null));
        assertThat(balance(bank)).isEqualByComparingTo("1500");

        publish(new ExpenseRegisteredEvent(7003L, DAY, null, pen("100"), pen("18"), pen("118"), true,
                PaymentMethod.BANK_ACCOUNT, bank, null));
        assertThat(balance(bank)).isEqualByComparingTo("1382");

        publish(new ExpenseRegisteredEvent(7004L, DAY, null, pen("100"), pen("18"), pen("118"), false,
                PaymentMethod.BANK_ACCOUNT, bank, null));
        publish(new ExpenseRegisteredEvent(7005L, DAY, null, pen("100"), pen("18"), pen("118"), true,
                PaymentMethod.PARTNER_CONTRIBUTION, null, 3L));
        assertThat(balance(bank)).isEqualByComparingTo("1382");

        publish(new ExpenseAnnulledEvent(7003L, DAY, true, bank, pen("118")));
        assertThat(balance(bank)).isEqualByComparingTo("1500");
    }

    @Test
    void rollsTheOperationBackWhenTheAccountCannotTakeTheMovement() {
        Long bank = bankAccount("993-2");
        var usd = new InvoicePaymentReceivedEvent(7010L, 1L, DAY, new Money(BigDecimal.TEN, Currency.USD),
                InvoicePaymentType.NET_DEPOSIT, bank);

        assertThatThrownBy(() -> publish(usd)).isInstanceOf(IllegalStateException.class);
        assertThat(balance(bank)).isEqualByComparingTo("1000");
    }

    @Test
    void takesFixedCostPaymentsAndTaxPaymentsOutOfTheAccount() {
        Long bank = bankAccount("993-3");

        publish(new FixedCostPaidEvent(7020L, 1L, FixedCostType.SALARY, DAY, pen("300"), bank));
        assertThat(balance(bank)).isEqualByComparingTo("700");

        publish(new FixedCostPaidEvent(7021L, 1L, FixedCostType.SALARY, DAY, pen("300"), null));
        assertThat(balance(bank)).isEqualByComparingTo("700");

        publish(new TaxReturnPaidEvent(7022L, DAY, pen("19.50"), bank));
        assertThat(balance(bank)).isEqualByComparingTo("680.50");
    }
}
