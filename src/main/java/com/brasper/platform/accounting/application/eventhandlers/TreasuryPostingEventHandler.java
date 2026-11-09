package com.brasper.platform.accounting.application.eventhandlers;

import com.brasper.platform.accounting.application.commandservices.LedgerCommandService;
import com.brasper.platform.accounting.domain.services.PostingRules;
import com.brasper.platform.staff.domain.events.FixedCostPaidEvent;
import com.brasper.platform.treasury.domain.events.LoanDisbursedEvent;
import com.brasper.platform.treasury.domain.events.LoanInstallmentPaidEvent;
import com.brasper.platform.treasury.domain.events.PartnerContributionRegisteredEvent;
import com.brasper.platform.treasury.domain.events.PayablePaidEvent;
import com.brasper.platform.treasury.domain.events.PayableRegisteredEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Entries for payments made elsewhere, posted in the transaction that made them. Staff payments arrive here too. */
@Component
class TreasuryPostingEventHandler {

    private final LedgerCommandService ledger;

    TreasuryPostingEventHandler(LedgerCommandService ledger) {
        this.ledger = ledger;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(FixedCostPaidEvent event) {
        PostingResults.check(ledger.handle(PostingRules.forFixedCostPayment(event)));
    }

    /** A payable that did not come from an expense is a new debt: the expense is booked now, the payment later. */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(PayableRegisteredEvent event) {
        PostingRules.forPayableRegistered(event).ifPresent(command -> PostingResults.check(ledger.handle(command)));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(PayablePaidEvent event) {
        PostingResults.check(ledger.handle(PostingRules.forPayablePayment(event)));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(LoanDisbursedEvent event) {
        PostingResults.check(ledger.handle(PostingRules.forLoanDisbursement(event)));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(LoanInstallmentPaidEvent event) {
        PostingResults.check(ledger.handle(PostingRules.forLoanInstallment(event)));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(PartnerContributionRegisteredEvent event) {
        PostingRules.forPartnerContribution(event).ifPresent(command -> PostingResults.check(ledger.handle(command)));
    }
}
