package com.brasper.platform.expenses.application.queryservices;

import com.brasper.platform.expenses.domain.model.aggregates.Expense;
import com.brasper.platform.expenses.domain.repositories.ExpenseRepository;
import com.brasper.platform.shared.domain.model.valueobjects.CostCategory;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExpenseQueryService {

    private final ExpenseRepository expenses;

    public ExpenseQueryService(ExpenseRepository expenses) {
        this.expenses = expenses;
    }

    /**
     * All filters are optional; annulled expenses are included (the caller sees the flag). Newest first.
     * shortcut: filters in memory, move them into the query if expenses reach thousands.
     */
    public List<Expense> findAll(Long projectId, Period spentIn, CostCategory category) {
        return expenses.findAll().stream()
                .filter(expense -> projectId == null || projectId.equals(expense.getProjectId()))
                .filter(expense -> spentIn == null || Period.from(expense.getSpentOn()).equals(spentIn))
                .filter(expense -> category == null || expense.getCategory() == category)
                .sorted(Comparator.comparing(Expense::getSpentOn).thenComparing(Expense::getId).reversed())
                .toList();
    }

    public Optional<Expense> findById(Long id) {
        return expenses.findById(id);
    }
}
