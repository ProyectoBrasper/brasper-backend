package com.brasper.platform.treasury.domain.model.commands;

import com.brasper.platform.shared.domain.model.valueobjects.Money;
import java.time.LocalDate;

public record RegisterPartnerContributionCommand(Long partnerId, LocalDate contributedOn, Money amount,
                                                boolean reimbursable, Long bankAccountId, Long expenseId,
                                                String concept) {
}
