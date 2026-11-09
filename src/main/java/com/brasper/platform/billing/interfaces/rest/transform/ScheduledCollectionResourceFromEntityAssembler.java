package com.brasper.platform.billing.interfaces.rest.transform;

import com.brasper.platform.billing.domain.model.aggregates.ScheduledCollection;
import com.brasper.platform.billing.interfaces.rest.resources.ScheduledCollectionResource;

public final class ScheduledCollectionResourceFromEntityAssembler {

    private ScheduledCollectionResourceFromEntityAssembler() {
    }

    public static ScheduledCollectionResource toResourceFromEntity(ScheduledCollection collection) {
        var week = collection.getExpectedWeek();
        return new ScheduledCollectionResource(collection.getId(), collection.getProjectId(),
                collection.getExpectedAmount().amount(), collection.getExpectedAmount().currency().name(),
                week.year(), week.month(), week.number(), collection.getStatus().name(), collection.getInvoiceId(),
                collection.getNotes());
    }
}
