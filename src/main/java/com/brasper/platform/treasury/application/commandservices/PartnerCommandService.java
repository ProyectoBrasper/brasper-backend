package com.brasper.platform.treasury.application.commandservices;

import com.brasper.platform.shared.application.result.Result;
import com.brasper.platform.treasury.domain.model.aggregates.Partner;
import com.brasper.platform.treasury.domain.model.commands.RegisterPartnerCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterPartnerContributionCommand;
import com.brasper.platform.treasury.domain.repositories.BankAccountRepository;
import com.brasper.platform.treasury.domain.repositories.PartnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartnerCommandService {

    private final PartnerRepository partners;
    private final BankAccountRepository accounts;

    public PartnerCommandService(PartnerRepository partners, BankAccountRepository accounts) {
        this.partners = partners;
        this.accounts = accounts;
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
        return Result.success(partners.save(partner));
    }
}
