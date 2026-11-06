package com.brasper.platform.expenses.interfaces.acl;

import java.math.BigDecimal;
import java.util.Map;

/**
 * The only way Treasury, Accounting and Reporting read Expenses. Annulled expenses never count.
 * shortcut: amounts of different currencies are added as plain numbers (see the Billing facade).
 */
public interface ExpensesContextFacade {

    /** Cost of the project's expenses without IGV: recoverable IGV is a tax credit, not a cost. */
    BigDecimal sumDirectCostByProject(Long projectId);

    /** Same cost split by {@code CostCategory} name; categories without expenses are left out. */
    Map<String, BigDecimal> fetchDirectCostByCategory(Long projectId);

    /** IGV of the invoices spent in the month, the credit Accounting can offset. */
    BigDecimal sumInputIgvByPeriod(int year, int month);

    /**
     * Total of expenses already paid from the company's bank or petty cash in the cash-flow week, by spending
     * date. Partner contributions are not company cash and payables paid later belong to Treasury.
     * shortcut: an expense settled after its spending date still counts in the spending week.
     */
    BigDecimal sumPaidByWeek(int year, int month, int week);
}
