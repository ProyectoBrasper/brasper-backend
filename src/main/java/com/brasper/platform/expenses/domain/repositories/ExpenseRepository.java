package com.brasper.platform.expenses.domain.repositories;

import com.brasper.platform.expenses.domain.model.aggregates.Expense;
import com.brasper.platform.shared.domain.model.valueobjects.CostCategory;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Port used by the application layer. Implemented by Spring Data in {@code infrastructure}. */
public interface ExpenseRepository {

    Expense save(Expense expense);

    Optional<Expense> findById(Long id);

    List<Expense> findAll();

    /**
     * Newest first. {@code projectId} and {@code category} are optional (null); {@code from} and {@code to} bound
     * the spending date, both inclusive and never null.
     */
    List<Expense> filter(Long projectId, CostCategory category, LocalDate from, LocalDate to, int page, int size);

    long countFiltered(Long projectId, CostCategory category, LocalDate from, LocalDate to);
}
