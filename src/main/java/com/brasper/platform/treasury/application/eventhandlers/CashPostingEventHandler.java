package com.brasper.platform.treasury.application.eventhandlers;

import com.brasper.platform.accounting.domain.events.TaxReturnPaidEvent;
import com.brasper.platform.billing.domain.events.InvoicePaymentReceivedEvent;
import com.brasper.platform.billing.domain.model.valueobjects.InvoicePaymentType;
import com.brasper.platform.expenses.domain.events.ExpenseAnnulledEvent;
import com.brasper.platform.expenses.domain.events.ExpenseRegisteredEvent;
import com.brasper.platform.expenses.domain.model.valueobjects.PaymentMethod;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.staff.domain.events.FixedCostPaidEvent;
import com.brasper.platform.treasury.application.commandservices.BankAccountCommandService;
import com.brasper.platform.treasury.domain.events.PartnerContributionRegisteredEvent;
import com.brasper.platform.treasury.domain.model.commands.RegisterCashMovementCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementDirection;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementSource;
import java.time.LocalDate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Registers the cash that Billing and Expenses move but do not record themselves: deposits of invoice payments and
 * expenses paid from a bank or petty cash account. Payables, loans and transfers already move cash in Treasury, so
 * they are not handled here and nothing is counted twice. Runs in the transaction of the event, so an account that
 * cannot take the movement (unknown, other currency) rolls the whole operation back.
 */
@Component
class CashPostingEventHandler {

    private final BankAccountCommandService accounts;

    CashPostingEventHandler(BankAccountCommandService accounts) {
        this.accounts = accounts;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(InvoicePaymentReceivedEvent event) {
        if (event.type() == InvoicePaymentType.RETENTION_CERTIFICATE || event.bankAccountId() == null) {
            return;
        }
        move(event.bankAccountId(), event.receivedOn(), MovementDirection.INFLOW, event.amount(),
                "Invoice payment", MovementSource.INVOICE_PAYMENT, event.paymentId());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(ExpenseRegisteredEvent event) {
        if (event.paid() && event.paymentMethod() != PaymentMethod.PARTNER_CONTRIBUTION && event.bankAccountId() != null) {
            move(event.bankAccountId(), event.spentOn(), MovementDirection.OUTFLOW, event.total(), "Expense",
                    MovementSource.EXPENSE, event.expenseId());
        }
    }

    /** Annulling a paid expense gives the money back to the account it came from. */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(ExpenseAnnulledEvent event) {
        if (event.paid() && event.bankAccountId() != null) {
            move(event.bankAccountId(), event.annulledOn(), MovementDirection.INFLOW, event.total(),
                    "Expense annulled", MovementSource.EXPENSE, event.expenseId());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(FixedCostPaidEvent event) {
        if (event.bankAccountId() != null) {
            move(event.bankAccountId(), event.paidOn(), MovementDirection.OUTFLOW, event.amount(), "Fixed cost payment",
                    MovementSource.FIXED_COST_PAYMENT, event.paymentId());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(TaxReturnPaidEvent event) {
        move(event.bankAccountId(), event.paidOn(), MovementDirection.OUTFLOW, event.amount(), "Tax return payment",
                MovementSource.TAX_PAYMENT, event.taxReturnId());
    }

    /** Money a partner put into an account. One that paid an expense out of pocket moved no company cash. */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(PartnerContributionRegisteredEvent event) {
        if (event.expenseId() == null && event.bankAccountId() != null) {
            move(event.bankAccountId(), event.contributedOn(), MovementDirection.INFLOW, event.amount(),
                    "Partner contribution", MovementSource.PARTNER_CONTRIBUTION, event.contributionId());
        }
    }

    private void move(Long bankAccountId, LocalDate on, MovementDirection direction, Money amount, String concept,
            MovementSource source, Long sourceId) {
        var result = accounts.handle(new RegisterCashMovementCommand(bankAccountId, on, direction, amount, concept,
                source, sourceId));
        if (result.isFailure()) {
            throw new IllegalStateException("Treasury rejected the cash movement: " + result.failure().orElseThrow());
        }
    }
}
