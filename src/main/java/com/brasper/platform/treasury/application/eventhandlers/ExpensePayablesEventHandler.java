package com.brasper.platform.treasury.application.eventhandlers;

import com.brasper.platform.expenses.domain.events.ExpenseAnnulledEvent;
import com.brasper.platform.expenses.domain.events.ExpenseRegisteredEvent;
import com.brasper.platform.expenses.domain.model.valueobjects.PaymentMethod;
import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.treasury.domain.model.aggregates.Payable;
import com.brasper.platform.treasury.domain.model.commands.CreatePayableCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.PayableType;
import com.brasper.platform.treasury.domain.repositories.PayableRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * An expense registered as pending is a debt: Treasury keeps it as a payable so the cash flow shows it and paying it
 * settles the expense. shortcut: it is scheduled in the week of the expense; reschedule it from the payables.
 */
@Component
class ExpensePayablesEventHandler {

    private final PayableRepository payables;

    ExpensePayablesEventHandler(PayableRepository payables) {
        this.payables = payables;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(ExpenseRegisteredEvent event) {
        if (event.paid() || event.paymentMethod() == PaymentMethod.PARTNER_CONTRIBUTION
                || payables.findByExpenseId(event.expenseId()).isPresent()) {
            return;
        }
        payables.save(new Payable(new CreatePayableCommand("Expense " + event.expenseId(), PayableType.SUPPLIER,
                event.total(), null, event.projectId(), event.expenseId(), CashFlowWeek.from(event.spentOn()))));
    }

    /** An annulled expense owes nothing. One whose payable already has payments has to be paid back first. */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(ExpenseAnnulledEvent event) {
        if (event.paid()) {
            return;
        }
        payables.findByExpenseId(event.expenseId()).ifPresent(payable -> {
            if (!payable.getPayments().isEmpty()) {
                throw new IllegalStateException("The payable of this expense already has payments");
            }
            payables.delete(payable);
        });
    }
}
