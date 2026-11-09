package com.brasper.platform.billing.application.queryservices;

import com.brasper.platform.billing.domain.model.aggregates.ScheduledCollection;
import com.brasper.platform.billing.domain.repositories.ScheduledCollectionRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ScheduledCollectionQueryService {

    private final ScheduledCollectionRepository collections;

    public ScheduledCollectionQueryService(ScheduledCollectionRepository collections) {
        this.collections = collections;
    }

    /** Every collection expected in the month, whatever its status, ordered by week. */
    public List<ScheduledCollection> findByPeriod(Period period) {
        return collections.findAllByPeriod(period);
    }
}
