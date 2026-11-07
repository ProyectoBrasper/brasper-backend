package com.brasper.platform.treasury.domain.events;

import com.brasper.platform.shared.domain.model.valueobjects.Money;
import java.time.LocalDate;

/**
 * {@code expenseId} is set when the partner paid an expense out of pocket: the expense already carries the
 * contribution, so it moves no cash and needs no entry of its own. {@code bankAccountId} is where the money went.
 */
public record PartnerContributionRegisteredEvent(Long contributionId, Long partnerId, LocalDate contributedOn,
        Money amount, Long bankAccountId, Long expenseId) { }
