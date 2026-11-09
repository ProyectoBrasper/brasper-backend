package com.brasper.platform.treasury.domain.repositories;

import com.brasper.platform.treasury.domain.model.aggregates.Payable;
import java.util.Optional;
import java.util.List;

public interface PayableRepository {
    Payable save(Payable payable);
    void delete(Payable payable);
    List<Payable> findAll();
    /** Every payable with its payments loaded, for sums that read the balance of each. */
    List<Payable> findAllWithPayments();
    Optional<Payable> findById(Long id);
    Optional<Payable> findByExpenseId(Long expenseId);
}
