package com.brasper.platform.expenses.application.queryservices;

import com.brasper.platform.expenses.domain.model.aggregates.Expense;
import com.brasper.platform.expenses.domain.repositories.ExpenseRepository;
import com.brasper.platform.shared.domain.model.valueobjects.CostCategory;
import com.brasper.platform.shared.domain.model.valueobjects.PageSpec;
import com.brasper.platform.shared.domain.model.valueobjects.PagedList;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.time.LocalDate;
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

    /** All filters are optional; annulled expenses are included (the caller sees the flag). Newest first. */
    public List<Expense> findAll(Long projectId, Period spentIn, CostCategory category) {
        return expenses.filter(projectId, category, from(spentIn), to(spentIn), 0, Integer.MAX_VALUE);
    }

    /** Same filters as {@link #findAll}, one page of them plus the total that match. */
    public PagedList<Expense> findPage(Long projectId, Period spentIn, CostCategory category, PageSpec page) {
        return new PagedList<>(expenses.filter(projectId, category, from(spentIn), to(spentIn), page.page(), page.size()),
                expenses.countFiltered(projectId, category, from(spentIn), to(spentIn)));
    }

    // Postgres cannot type a null date parameter, so "no period" is the widest range instead.
    private static LocalDate from(Period period) {
        return period == null ? LocalDate.of(1900, 1, 1) : period.startDate();
    }

    private static LocalDate to(Period period) {
        return period == null ? LocalDate.of(9999, 12, 31) : period.endDate();
    }

    public Optional<Expense> findById(Long id) {
        return expenses.findById(id);
    }
}
