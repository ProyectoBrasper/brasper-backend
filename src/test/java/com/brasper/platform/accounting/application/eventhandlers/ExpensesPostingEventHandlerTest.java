package com.brasper.platform.accounting.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.accounting.application.queryservices.LedgerQueryService;
import com.brasper.platform.accounting.domain.model.aggregates.JournalEntry;
import com.brasper.platform.accounting.domain.model.valueobjects.EntrySource;
import com.brasper.platform.expenses.domain.events.ExpenseAnnulledEvent;
import com.brasper.platform.expenses.domain.events.ExpenseRegisteredEvent;
import com.brasper.platform.expenses.domain.model.valueobjects.PaymentMethod;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import com.brasper.platform.treasury.application.commandservices.BankAccountCommandService;
import com.brasper.platform.treasury.domain.model.commands.OpenBankAccountCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountNumber;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountType;
import com.brasper.platform.treasury.domain.model.valueobjects.Bank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

/** Not @Transactional on purpose: the handlers run when the publishing transaction commits. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ExpensesPostingEventHandlerTest {

    private static final LocalDate DAY = LocalDate.of(2034, 6, 10);

    @Autowired private ApplicationEventPublisher publisher;
    @Autowired private TransactionTemplate transaction;
    @Autowired private LedgerQueryService ledger;

    @Autowired private BankAccountCommandService bankAccounts;

    private Long bankAccount() {
        return bankAccounts.handle(new OpenBankAccountCommand(Bank.BCP, new AccountNumber("992-2"),
                AccountType.CHECKING, Currency.PEN, pen("0"), DAY)).success().orElseThrow().getId();
    }

    private static Money pen(String amount) {
        return new Money(new BigDecimal(amount), Currency.PEN);
    }

    private void publish(Object event) {
        transaction.executeWithoutResult(status -> publisher.publishEvent(event));
    }

    private List<JournalEntry> entries(EntrySource source, Long sourceId) {
        return ledger.findEntriesByPeriod(new Period(2034, 6)).stream()
                .filter(e -> e.getSource() == source && sourceId.equals(e.getSourceId())).toList();
    }

    @Test
    void postsARegisteredExpenseOnceAndReversesItWhenAnnulled() {
        Long bank = bankAccount();
        var event = new ExpenseRegisteredEvent(8001L, DAY, null, pen("100"), pen("18"), pen("118"), true,
                PaymentMethod.BANK_ACCOUNT, bank, null);
        publish(event);
        publish(event);

        assertThat(entries(EntrySource.EXPENSE, 8001L)).hasSize(1);

        publish(new ExpenseAnnulledEvent(8001L, DAY, true, bank, pen("118")));
        publish(new ExpenseAnnulledEvent(8999L, DAY, false, null, pen("118")));

        assertThat(entries(EntrySource.EXPENSE, 8001L).getFirst().isReversed()).isTrue();
    }
}
