package com.brasper.platform.accounting.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.accounting.application.queryservices.LedgerQueryService;
import com.brasper.platform.accounting.domain.model.aggregates.JournalEntry;
import com.brasper.platform.accounting.domain.model.valueobjects.EntrySource;
import com.brasper.platform.billing.domain.events.InvoiceIssuedEvent;
import com.brasper.platform.billing.domain.events.InvoicePaymentReceivedEvent;
import com.brasper.platform.billing.domain.events.InvoiceVoidedEvent;
import com.brasper.platform.billing.domain.model.valueobjects.InvoicePaymentType;
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
class BillingPostingEventHandlerTest {

    private static final LocalDate DAY = LocalDate.of(2033, 5, 10);
    private static final Period PERIOD = new Period(2033, 5);

    @Autowired private ApplicationEventPublisher publisher;
    @Autowired private TransactionTemplate transaction;
    @Autowired private LedgerQueryService ledger;

    @Autowired private BankAccountCommandService bankAccounts;

    private Long bankAccount() {
        return bankAccounts.handle(new OpenBankAccountCommand(Bank.BCP, new AccountNumber("992-1"),
                AccountType.CHECKING, Currency.PEN, pen("0"), DAY)).success().orElseThrow().getId();
    }

    private static Money pen(String amount) {
        return new Money(new BigDecimal(amount), Currency.PEN);
    }

    private void publish(Object event) {
        transaction.executeWithoutResult(status -> publisher.publishEvent(event));
    }

    private List<JournalEntry> entries(EntrySource source, Long sourceId) {
        return ledger.findEntriesByPeriod(PERIOD).stream()
                .filter(e -> e.getSource() == source && sourceId.equals(e.getSourceId())).toList();
    }

    @Test
    void postsAnIssuedInvoiceAndItsPaymentAndReversesItWhenVoided() {
        Long bank = bankAccount();
        publish(new InvoiceIssuedEvent(9001L, null, DAY, pen("1000"), pen("180"), pen("1180"), false));
        publish(new InvoicePaymentReceivedEvent(9002L, 9001L, DAY, pen("500"), InvoicePaymentType.NET_DEPOSIT, bank));
        publish(new InvoicePaymentReceivedEvent(9003L, 9001L, DAY, pen("35"), InvoicePaymentType.RETENTION_CERTIFICATE, null));

        assertThat(entries(EntrySource.INVOICE_ISSUED, 9001L)).hasSize(1);
        assertThat(entries(EntrySource.INVOICE_PAYMENT, 9002L)).hasSize(1);
        assertThat(entries(EntrySource.INVOICE_PAYMENT, 9003L)).isEmpty();

        publish(new InvoiceVoidedEvent(9001L, DAY));
        assertThat(entries(EntrySource.INVOICE_ISSUED, 9001L).getFirst().isReversed()).isTrue();
        assertThat(entries(EntrySource.REVERSAL, entries(EntrySource.INVOICE_ISSUED, 9001L).getFirst().getId())).hasSize(1);
    }

    @Test
    void repeatingAnEventPostsNothingTwiceAndAnInvoiceIssuedBeforeEventsCanBeVoided() {
        var event = new InvoiceIssuedEvent(9010L, null, DAY, pen("100"), pen("18"), pen("118"), false);
        publish(event);
        publish(event);
        publish(new InvoiceVoidedEvent(9999L, DAY));

        assertThat(entries(EntrySource.INVOICE_ISSUED, 9010L)).hasSize(1);
    }

    @Test
    void rollsTheOperationBackWhenTheLedgerCannotTakeTheEntry() {
        var unbalanced = new InvoiceIssuedEvent(9020L, null, DAY, pen("100"), pen("18"), pen("500"), false);

        assertThatThrownBy(() -> publish(unbalanced)).isInstanceOf(IllegalStateException.class);
        assertThat(entries(EntrySource.INVOICE_ISSUED, 9020L)).isEmpty();
    }
}
