package com.brasper.platform.expenses.domain.repositories;

import com.brasper.platform.expenses.domain.model.aggregates.Expense;
import java.util.List;
import java.util.Optional;

/** Port used by the application layer. Implemented by Spring Data in {@code infrastructure}. */
public interface ExpenseRepository {

    Expense save(Expense expense);

    Optional<Expense> findById(Long id);

    List<Expense> findAll();
}
