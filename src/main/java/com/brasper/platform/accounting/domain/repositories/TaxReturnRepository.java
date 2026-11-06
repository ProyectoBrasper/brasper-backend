package com.brasper.platform.accounting.domain.repositories;

import com.brasper.platform.accounting.domain.model.aggregates.TaxReturn;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.util.List;
import java.util.Optional;

public interface TaxReturnRepository {
    TaxReturn save(TaxReturn entity);
    Optional<TaxReturn> findById(Long id);
    List<TaxReturn> findAll();
    Optional<TaxReturn> findByPeriod(Period period);
    boolean existsByPeriod(Period period);
}
