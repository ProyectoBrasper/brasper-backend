package com.brasper.platform.expenses.infrastructure.persistence.jpa;

import com.brasper.platform.expenses.domain.model.aggregates.Expense;
import com.brasper.platform.expenses.domain.repositories.ExpenseRepository;
import com.brasper.platform.shared.domain.model.valueobjects.CostCategory;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseJpaRepository extends JpaRepository<Expense, Long>, ExpenseRepository {

    String FILTERS = """
            where (:projectId is null or e.projectId = :projectId) and (:category is null or e.category = :category)
              and e.spentOn between :from and :to""";

    @Query("select e from Expense e " + FILTERS + " order by e.spentOn desc, e.id desc")
    List<Expense> filterPage(@Param("projectId") Long projectId, @Param("category") CostCategory category,
            @Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);

    @Override
    default List<Expense> filter(Long projectId, CostCategory category, LocalDate from, LocalDate to, int page,
            int size) {
        return filterPage(projectId, category, from, to, PageRequest.of(page, size));
    }

    @Override
    @Query("select count(e) from Expense e " + FILTERS)
    long countFiltered(@Param("projectId") Long projectId, @Param("category") CostCategory category,
            @Param("from") LocalDate from, @Param("to") LocalDate to);
}
