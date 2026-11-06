package com.brasper.platform.treasury.domain.repositories;

import com.brasper.platform.treasury.domain.model.aggregates.Payable;
import java.util.Optional;

public interface PayableRepository {
    Payable save(Payable payable);
    Optional<Payable> findById(Long id);
    Optional<Payable> findByExpenseId(Long expenseId);
}
