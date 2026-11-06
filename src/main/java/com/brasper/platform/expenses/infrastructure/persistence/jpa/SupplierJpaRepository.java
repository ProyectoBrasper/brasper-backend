package com.brasper.platform.expenses.infrastructure.persistence.jpa;

import com.brasper.platform.expenses.domain.model.aggregates.Supplier;
import com.brasper.platform.expenses.domain.repositories.SupplierRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierJpaRepository extends JpaRepository<Supplier, Long>, SupplierRepository {
}
