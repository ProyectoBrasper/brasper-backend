package com.brasper.platform.accounting.application.eventhandlers;

import com.brasper.platform.accounting.application.commandservices.LedgerCommandFailure;
import com.brasper.platform.accounting.application.commandservices.LedgerCommandService;
import com.brasper.platform.accounting.domain.model.commands.RegisterJournalEntryCommand;
import com.brasper.platform.accounting.domain.model.commands.ReverseJournalEntryCommand;
import com.brasper.platform.accounting.domain.model.valueobjects.EntrySource;
import com.brasper.platform.accounting.domain.repositories.JournalEntryRepository;
import com.brasper.platform.accounting.domain.services.PostingRules;
import com.brasper.platform.billing.domain.events.InvoiceIssuedEvent;
import com.brasper.platform.billing.domain.events.InvoicePaymentReceivedEvent;
import com.brasper.platform.billing.domain.events.InvoiceVoidedEvent;
import com.brasper.platform.shared.application.result.Result;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Turns Billing events into journal entries inside the transaction that raised them: if the ledger cannot take the
 * entry (a missing or inactive account) the whole operation is rolled back, so Billing and the ledger never disagree.
 */
@Component
class BillingPostingEventHandler {

    private final LedgerCommandService ledger;
    private final JournalEntryRepository entries;

    BillingPostingEventHandler(LedgerCommandService ledger, JournalEntryRepository entries) {
        this.ledger = ledger;
        this.entries = entries;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(InvoiceIssuedEvent event) {
        register(PostingRules.forInvoiceIssued(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(InvoicePaymentReceivedEvent event) {
        PostingRules.forInvoicePayment(event).ifPresent(this::register);
    }

    /** An invoice issued before the events existed has no entry to reverse. */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(InvoiceVoidedEvent event) {
        entries.findBySourceAndSourceId(EntrySource.INVOICE_ISSUED, event.invoiceId())
                .ifPresent(entry -> check(ledger.handle(new ReverseJournalEntryCommand(entry.getId(), event.voidedOn()))));
    }

    private void register(RegisterJournalEntryCommand command) {
        check(ledger.handle(command));
    }

    /** A repeated source means the entry is already there, which is what we want. */
    private static void check(Result<?, LedgerCommandFailure> result) {
        if (result.isFailure() && result.failure().orElseThrow() != LedgerCommandFailure.DUPLICATE_SOURCE) {
            throw new IllegalStateException("The ledger rejected the entry: " + result.failure().orElseThrow());
        }
    }
}
