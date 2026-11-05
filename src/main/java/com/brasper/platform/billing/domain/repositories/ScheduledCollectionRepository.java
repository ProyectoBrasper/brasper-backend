package com.brasper.platform.billing.domain.repositories;

import com.brasper.platform.billing.domain.model.aggregates.ScheduledCollection;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.util.List;
import java.util.Optional;

/** Port used by the application layer. Implemented by Spring Data in {@code infrastructure}. */
public interface ScheduledCollectionRepository {

    ScheduledCollection save(ScheduledCollection collection);

    Optional<ScheduledCollection> findById(Long id);

    /** Every collection expected in that month, whatever its status. */
    List<ScheduledCollection> findAllByPeriod(Period period);
}
