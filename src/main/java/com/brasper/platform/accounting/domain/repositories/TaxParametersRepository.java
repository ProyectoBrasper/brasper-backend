package com.brasper.platform.accounting.domain.repositories;

import com.brasper.platform.accounting.domain.model.aggregates.TaxParameters;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaxParametersRepository {
    TaxParameters save(TaxParameters entity);
    Optional<TaxParameters> findById(Long id);
    List<TaxParameters> findAll();
    Optional<TaxParameters> findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDate date);
}
