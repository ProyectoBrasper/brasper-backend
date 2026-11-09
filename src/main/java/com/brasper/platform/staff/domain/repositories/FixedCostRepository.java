package com.brasper.platform.staff.domain.repositories;

import com.brasper.platform.staff.domain.model.aggregates.FixedCost;
import com.brasper.platform.staff.domain.model.valueobjects.CostCode;
import java.util.List;
import java.util.Optional;
public interface FixedCostRepository {
    FixedCost save(FixedCost fixedCost);
    Optional<FixedCost> findById(Long id);
    Optional<FixedCost> findByCode(CostCode code);
    boolean existsByCode(CostCode code);
    List<FixedCost> findAll();
    List<FixedCost> findAllByActive(boolean active);
    List<FixedCost> findAllByEmployeeId(Long employeeId);
    /** Oldest first. {@code active} is optional (null). */
    List<FixedCost> filter(Boolean active, int page, int size);
    long countFiltered(Boolean active);
}
