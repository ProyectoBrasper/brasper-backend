package com.brasper.platform.expenses.application.eventhandlers;

import com.brasper.platform.expenses.domain.repositories.ExpenseRepository;
import com.brasper.platform.treasury.domain.events.PayableSettledEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Treasury paid the payable of a pending expense, so the expense is paid. */
@Component
class PayableSettlementEventHandler {

    private final ExpenseRepository expenses;

    PayableSettlementEventHandler(ExpenseRepository expenses) {
        this.expenses = expenses;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(PayableSettledEvent event) {
        expenses.findById(event.expenseId()).filter(expense -> !expense.isAnnulled()).ifPresent(expense -> {
            expense.markAsPaid();
            expenses.save(expense);
        });
    }
}
