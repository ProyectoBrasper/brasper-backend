package com.brasper.platform.expenses.interfaces.rest.transform;

import com.brasper.platform.expenses.domain.model.aggregates.Expense;
import com.brasper.platform.expenses.interfaces.rest.resources.ExpenseResource;

public final class ExpenseResourceFromEntityAssembler {

    private ExpenseResourceFromEntityAssembler() {
    }

    public static ExpenseResource toResourceFromEntity(Expense expense) {
        var amounts = expense.getAmounts();
        return new ExpenseResource(expense.getId(), expense.getSpentOn(), expense.getProjectId(),
                expense.getSupplierId(), expense.getCategory().name(), expense.getDescription(),
                expense.getVoucher().type().name(), expense.getVoucher().number(), amounts.taxableBase().amount(),
                amounts.igv().amount(), amounts.total().amount(), amounts.total().currency().name(),
                expense.getPaymentMethod().name(), expense.getBankAccountId(), expense.getPartnerId(),
                expense.getPaymentStatus().name(), expense.isAnnulled());
    }
}
