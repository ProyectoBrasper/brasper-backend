package com.brasper.platform.accounting.application.eventhandlers;

import com.brasper.platform.accounting.application.commandservices.LedgerCommandService;
import com.brasper.platform.accounting.domain.model.commands.RegisterJournalEntryCommand;
import com.brasper.platform.accounting.domain.repositories.JournalEntryRepository;
import com.brasper.platform.accounting.domain.services.PostingRules;
import com.brasper.platform.projects.domain.events.ProjectCompletedEvent;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * When a project completes, the customer advances taken for it become sales. The amount is what the project's advance
 * invoices credited to 122, read from the ledger itself.
 */
@Component
class ProjectsPostingEventHandler {

    private final LedgerCommandService ledger;
    private final JournalEntryRepository entries;

    ProjectsPostingEventHandler(LedgerCommandService ledger, JournalEntryRepository entries) {
        this.ledger = ledger;
        this.entries = entries;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(ProjectCompletedEvent event) {
        BigDecimal advances = entries.sumCreditBalanceByAccountAndProject(PostingRules.CUSTOMER_ADVANCES, event.projectId());
        PostingRules.forProjectCompleted(event, advances).ifPresent((RegisterJournalEntryCommand command) ->
                PostingResults.check(ledger.handle(command)));
    }
}
