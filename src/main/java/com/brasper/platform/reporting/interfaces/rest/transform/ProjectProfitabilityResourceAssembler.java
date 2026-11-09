package com.brasper.platform.reporting.interfaces.rest.transform;

import com.brasper.platform.reporting.domain.model.readmodels.ProjectProfitability;
import com.brasper.platform.reporting.interfaces.rest.resources.ProjectProfitabilityResource;

public final class ProjectProfitabilityResourceAssembler {

    private ProjectProfitabilityResourceAssembler() { }

    public static ProjectProfitabilityResource toResource(ProjectProfitability p) {
        return new ProjectProfitabilityResource(p.projectId(), p.code(), p.name(), p.clientName(), p.status(),
                p.currency(), p.budget(), p.estimatedCost(), p.actualCost(), p.actualCostByCategory(),
                p.costDeviation(), p.salesCommission(), p.grossProfit(), p.margin(), p.accumulatedProgress(),
                p.recognizedProfit(), p.invoiced(), p.collected());
    }
}
