package com.brasper.platform.treasury.interfaces.rest.transform;

import com.brasper.platform.treasury.domain.model.aggregates.Partner;
import com.brasper.platform.treasury.interfaces.rest.resources.PartnerResource;
import com.brasper.platform.treasury.interfaces.rest.resources.PartnerResource.ContributionResource;
import java.math.BigDecimal;
import java.util.List;

public final class PartnerResourceAssembler {

    private PartnerResourceAssembler() {
    }

    public static PartnerResource toResource(Partner partner) {
        List<ContributionResource> contributions = partner.getContributions().stream()
                .map(c -> new ContributionResource(c.getId(), c.getContributedOn(), c.getAmount().amount(),
                        c.getAmount().currency().name(), c.isReimbursable(), c.getBankAccountId(),
                        c.getExpenseId(), c.getConcept()))
                .toList();
        BigDecimal total = contributions.stream().map(ContributionResource::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PartnerResource(partner.getId(), partner.getFullName(), partner.getRole(), total, contributions);
    }
}
