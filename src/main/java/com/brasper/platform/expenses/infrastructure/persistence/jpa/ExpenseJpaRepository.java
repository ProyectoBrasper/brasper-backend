package com.brasper.platform.expenses.infrastructure.persistence.jpa;

import com.brasper.platform.expenses.domain.model.aggregates.Expense;
import com.brasper.platform.expenses.domain.repositories.ExpenseRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseJpaRepository extends JpaRepository<Expense, Long>, ExpenseRepository {
}
