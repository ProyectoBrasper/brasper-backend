package com.brasper.platform.accounting.application.eventhandlers;

import com.brasper.platform.accounting.application.commandservices.LedgerCommandService;
import com.brasper.platform.accounting.domain.model.commands.ReverseJournalEntryCommand;
import com.brasper.platform.accounting.domain.model.valueobjects.EntrySource;
import com.brasper.platform.accounting.domain.repositories.JournalEntryRepository;
import com.brasper.platform.accounting.domain.services.PostingRules;
import com.brasper.platform.expenses.domain.events.ExpenseAnnulledEvent;
import com.brasper.platform.expenses.domain.events.ExpenseRegisteredEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Same rule as the Billing handler: the entry is posted in the transaction that registered the expense. */
@Component
class ExpensesPostingEventHandler {

    private final LedgerCommandService ledger;
    private final JournalEntryRepository entries;

    ExpensesPostingEventHandler(LedgerCommandService ledger, JournalEntryRepository entries) {
        this.ledger = ledger;
        this.entries = entries;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(ExpenseRegisteredEvent event) {
        PostingResults.check(ledger.handle(PostingRules.forExpense(event)));
    }

    /** An expense registered before the events existed has no entry to reverse. */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(ExpenseAnnulledEvent event) {
        entries.findBySourceAndSourceId(EntrySource.EXPENSE, event.expenseId()).ifPresent(entry ->
                PostingResults.check(ledger.handle(new ReverseJournalEntryCommand(entry.getId(), event.annulledOn()))));
    }
}
