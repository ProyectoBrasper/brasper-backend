package com.brasper.platform.projects.interfaces.rest.transform;

import com.brasper.platform.projects.domain.model.entities.ProgressRecord;
import com.brasper.platform.projects.interfaces.rest.resources.ProgressRecordResource;

public final class ProgressRecordResourceFromEntityAssembler {

    private ProgressRecordResourceFromEntityAssembler() {
    }

    public static ProgressRecordResource toResourceFromEntity(ProgressRecord record) {
        return new ProgressRecordResource(record.getId(), record.getPeriod().year(), record.getPeriod().month(),
                record.getPercentage().value());
    }
}
