package com.brasper.platform.expenses.domain.repositories;

import com.brasper.platform.expenses.domain.model.aggregates.Supplier;
import com.brasper.platform.shared.domain.model.valueobjects.Ruc;
import java.util.List;
import java.util.Optional;

/** Port used by the application layer. Implemented by Spring Data in {@code infrastructure}. */
public interface SupplierRepository {

    Supplier save(Supplier supplier);

    Optional<Supplier> findById(Long id);

    boolean existsByRuc(Ruc ruc);

    List<Supplier> findAll();
}
