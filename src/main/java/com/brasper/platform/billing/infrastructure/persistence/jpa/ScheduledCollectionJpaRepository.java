package com.brasper.platform.billing.infrastructure.persistence.jpa;

import com.brasper.platform.billing.domain.model.aggregates.ScheduledCollection;
import com.brasper.platform.billing.domain.repositories.ScheduledCollectionRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduledCollectionJpaRepository
        extends JpaRepository<ScheduledCollection, Long>, ScheduledCollectionRepository {

    List<ScheduledCollection> findAllByExpectedYearAndExpectedMonthOrderByExpectedWeekAscIdAsc(int year, int month);

    /** The week is stored as three columns, so the derived query takes the period apart. */
    @Override
    default List<ScheduledCollection> findAllByPeriod(Period period) {
        return findAllByExpectedYearAndExpectedMonthOrderByExpectedWeekAscIdAsc(period.year(), period.month());
    }
}
