package com.brasper.platform.treasury.application.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.treasury.domain.model.aggregates.BankAccount;
import com.brasper.platform.treasury.domain.model.aggregates.Partner;
import com.brasper.platform.treasury.domain.model.commands.RegisterPartnerCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterPartnerContributionCommand;
import com.brasper.platform.treasury.domain.repositories.BankAccountRepository;
import com.brasper.platform.treasury.domain.repositories.PartnerRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class PartnerCommandServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 6);

    private final PartnerRepository partners = mock(PartnerRepository.class);
    private final BankAccountRepository accounts = mock(BankAccountRepository.class);
    private final PartnerCommandService service = new PartnerCommandService(partners, accounts, mock(ApplicationEventPublisher.class));

    private static Money pen(String amount) {
        return new Money(new BigDecimal(amount), Currency.PEN);
    }

    private static RegisterPartnerContributionCommand contribution(Money amount, Long bankAccountId) {
        return new RegisterPartnerContributionCommand(1L, DAY, amount, true, bankAccountId, null, "tools");
    }

    @Test
    void registersAPartnerAndRejectsADuplicateName() {
        when(partners.save(any(Partner.class))).thenAnswer(call -> call.getArgument(0));
        when(partners.existsByFullName("Ana Perez")).thenReturn(false, true);
        RegisterPartnerCommand command = new RegisterPartnerCommand(" Ana Perez ", "Engineer");

        assertThat(service.handle(command).success().orElseThrow().getFullName()).isEqualTo("Ana Perez");
        assertThat(service.handle(command).failure()).contains(PartnerCommandFailure.DUPLICATE_PARTNER);
    }

    @Test
    void registersAContributionWithAndWithoutABankAccount() {
        Partner partner = new Partner(new RegisterPartnerCommand("Ana Perez", null));
        when(partners.findById(1L)).thenReturn(Optional.of(partner));
        when(partners.save(any(Partner.class))).thenAnswer(call -> call.getArgument(0));
        when(accounts.findById(5L)).thenReturn(Optional.of(mock(BankAccount.class)));

        service.handle(contribution(pen("100"), null)).success().orElseThrow();
        service.handle(contribution(pen("50"), 5L)).success().orElseThrow();

        assertThat(partner.getContributions()).hasSize(2);
    }

    @Test
    void rejectsContributionsForAMissingPartnerAccountOrAmount() {
        Partner partner = new Partner(new RegisterPartnerCommand("Ana Perez", null));
        when(partners.findById(1L)).thenReturn(Optional.of(partner));
        when(partners.findById(2L)).thenReturn(Optional.empty());
        when(accounts.findById(9L)).thenReturn(Optional.empty());

        assertThat(service.handle(new RegisterPartnerContributionCommand(2L, DAY, pen("1"), false, null, null, null))
                .failure()).contains(PartnerCommandFailure.PARTNER_NOT_FOUND);
        assertThat(service.handle(contribution(pen("0"), null)).failure()).contains(PartnerCommandFailure.INVALID_AMOUNT);
        assertThat(service.handle(contribution(pen("10"), 9L)).failure())
                .contains(PartnerCommandFailure.CONTRIBUTION_ACCOUNT_NOT_FOUND);
        assertThat(partner.getContributions()).isEmpty();
        verify(partners, never()).save(any());
    }
}
