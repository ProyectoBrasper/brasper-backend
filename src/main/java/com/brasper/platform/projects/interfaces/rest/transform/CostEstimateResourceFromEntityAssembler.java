package com.brasper.platform.projects.interfaces.rest.transform;

import com.brasper.platform.projects.domain.model.entities.CostEstimate;
import com.brasper.platform.projects.interfaces.rest.resources.CostEstimateResource;

public final class CostEstimateResourceFromEntityAssembler {

    private CostEstimateResourceFromEntityAssembler() {
    }

    public static CostEstimateResource toResourceFromEntity(CostEstimate estimate) {
        return new CostEstimateResource(estimate.getId(), estimate.getCategory().name(),
                estimate.getEstimatedAmount());
    }
}
