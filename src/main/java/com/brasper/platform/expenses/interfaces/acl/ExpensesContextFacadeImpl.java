package com.brasper.platform.expenses.interfaces.acl;

import com.brasper.platform.expenses.application.queryservices.ExpenseQueryService;
import com.brasper.platform.expenses.domain.model.aggregates.Expense;
import com.brasper.platform.expenses.domain.model.valueobjects.PaymentMethod;
import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.PaymentStatus;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

/**
 * Lives in {@code interfaces} (the UML puts it in {@code application}) because the layer rules forbid
 * application from depending on interfaces, where the facade contract lives.
 */
@Service
public class ExpensesContextFacadeImpl implements ExpensesContextFacade {

    private final ExpenseQueryService expenses;

    public ExpensesContextFacadeImpl(ExpenseQueryService expenses) {
        this.expenses = expenses;
    }

    @Override
    public BigDecimal sumDirectCostByProject(Long projectId) {
        return sum(active(projectId, null).map(expense -> expense.getAmounts().taxableBase().amount()));
    }

    @Override
    public Map<String, BigDecimal> fetchDirectCostByCategory(Long projectId) {
        return active(projectId, null).collect(Collectors.groupingBy(expense -> expense.getCategory().name(),
                TreeMap::new, Collectors.reducing(BigDecimal.ZERO,
                        expense -> expense.getAmounts().taxableBase().amount(), BigDecimal::add)));
    }

    @Override
    public BigDecimal sumInputIgvByPeriod(int year, int month) {
        return sum(active(null, new Period(year, month)).filter(Expense::generatesTaxCredit)
                .map(expense -> expense.getAmounts().igv().amount()));
    }

    @Override
    public BigDecimal sumPaidByWeek(int year, int month, int week) {
        CashFlowWeek cashFlowWeek = new CashFlowWeek(year, month, week);
        return sum(active(null, cashFlowWeek.period())
                .filter(expense -> cashFlowWeek.contains(expense.getSpentOn()))
                .filter(expense -> expense.getPaymentStatus() == PaymentStatus.PAID)
                .filter(expense -> expense.getPaymentMethod() != PaymentMethod.PARTNER_CONTRIBUTION)
                .map(expense -> expense.getAmounts().total().amount()));
    }

    /** With a project id only its direct-cost expenses; without one, every expense in the period. */
    private Stream<Expense> active(Long projectId, Period period) {
        return expenses.findAll(projectId, period, null).stream().filter(expense -> !expense.isAnnulled());
    }

    private static BigDecimal sum(Stream<BigDecimal> amounts) {
        return amounts.reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
