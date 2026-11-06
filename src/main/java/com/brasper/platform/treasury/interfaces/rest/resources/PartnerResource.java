package com.brasper.platform.treasury.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** {@code totalContributed} adds the amounts as plain numbers, whatever their currency. */
public record PartnerResource(Long id, String fullName, String role, BigDecimal totalContributed,
                              List<ContributionResource> contributions) {

    public record ContributionResource(Long id, LocalDate contributedOn, BigDecimal amount, String currency,
                                       boolean reimbursable, Long bankAccountId, Long expenseId, String concept) {
    }
}
