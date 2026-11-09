package com.brasper.platform.accounting.infrastructure.persistence.jpa;

import com.brasper.platform.accounting.domain.model.aggregates.TaxReturn;
import com.brasper.platform.accounting.domain.repositories.TaxReturnRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxReturnJpaRepository extends JpaRepository<TaxReturn, Long>, TaxReturnRepository {
    Optional<TaxReturn> findByPeriodYearAndPeriodMonth(int year, int month);
    boolean existsByPeriodYearAndPeriodMonth(int year, int month);
    @Override default Optional<TaxReturn> findByPeriod(Period period) {
        return findByPeriodYearAndPeriodMonth(period.year(), period.month());
    }
    @Override default boolean existsByPeriod(Period period) {
        return existsByPeriodYearAndPeriodMonth(period.year(), period.month());
    }
}
