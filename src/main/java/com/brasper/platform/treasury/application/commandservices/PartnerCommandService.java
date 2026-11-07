package com.brasper.platform.treasury.application.commandservices;

import com.brasper.platform.shared.application.result.Result;
import com.brasper.platform.treasury.domain.events.PartnerContributionRegisteredEvent;
import com.brasper.platform.treasury.domain.model.aggregates.Partner;
import com.brasper.platform.treasury.domain.model.commands.RegisterPartnerCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterPartnerContributionCommand;
import com.brasper.platform.treasury.domain.repositories.BankAccountRepository;
import com.brasper.platform.treasury.domain.repositories.PartnerRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartnerCommandService {

    private final PartnerRepository partners;
    private final BankAccountRepository accounts;
    private final ApplicationEventPublisher events;

    public PartnerCommandService(PartnerRepository partners, BankAccountRepository accounts,
            ApplicationEventPublisher events) {
        this.partners = partners;
        this.accounts = accounts;
        this.events = events;
    }

    @Transactional
    public Result<Partner, PartnerCommandFailure> handle(RegisterPartnerCommand command) {
        if (command.fullName() != null && partners.existsByFullName(command.fullName().strip())) {
            return Result.failure(PartnerCommandFailure.DUPLICATE_PARTNER);
        }
        return Result.success(partners.save(new Partner(command)));
    }

    /** Records the contribution only; it does not move cash, the bank account id just says where the money went. */
    @Transactional
    public Result<Partner, PartnerCommandFailure> handle(RegisterPartnerContributionCommand command) {
        Partner partner = partners.findById(command.partnerId()).orElse(null);
        if (partner == null) {
            return Result.failure(PartnerCommandFailure.PARTNER_NOT_FOUND);
        }
        if (command.amount() == null || !command.amount().isPositive()) {
            return Result.failure(PartnerCommandFailure.INVALID_AMOUNT);
        }
        if (command.bankAccountId() != null && accounts.findById(command.bankAccountId()).isEmpty()) {
            return Result.failure(PartnerCommandFailure.CONTRIBUTION_ACCOUNT_NOT_FOUND);
        }
        partner.registerContribution(command);
        Partner saved = partners.save(partner);
        // Read the contribution back from the saved aggregate: that instance is the persisted one, with its id.
        var contribution = saved.getContributions().getLast();
        events.publishEvent(new PartnerContributionRegisteredEvent(contribution.getId(), saved.getId(),
                command.contributedOn(), command.amount(), command.bankAccountId(), command.expenseId()));
        return Result.success(saved);
    }
}
