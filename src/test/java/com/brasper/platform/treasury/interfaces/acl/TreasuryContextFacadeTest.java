package com.brasper.platform.treasury.interfaces.acl;

import static org.assertj.core.api.Assertions.assertThat;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.treasury.application.commandservices.BankAccountCommandService;
import com.brasper.platform.treasury.application.commandservices.PartnerCommandService;
import com.brasper.platform.treasury.domain.model.commands.OpenBankAccountCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterPartnerCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountNumber;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountType;
import com.brasper.platform.treasury.domain.model.valueobjects.Bank;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class TreasuryContextFacadeTest {

    @Autowired
    private TreasuryContextFacade facade;
    @Autowired
    private BankAccountCommandService accounts;
    @Autowired
    private PartnerCommandService partners;

    @Test
    void answersAboutBankAccounts() {
        Long id = accounts.handle(new OpenBankAccountCommand(Bank.BANCO_DE_LA_NACION, new AccountNumber("001-1"),
                AccountType.DETRACTIONS, Currency.PEN, Money.zero(Currency.PEN), LocalDate.of(2026, 11, 1)))
                .success().orElseThrow().getId();

        assertThat(facade.existsBankAccountById(id)).isTrue();
        assertThat(facade.fetchBankAccountType(id)).contains("DETRACTIONS");
        assertThat(facade.existsBankAccountById(999_999L)).isFalse();
        assertThat(facade.fetchBankAccountType(999_999L)).isEmpty();
    }

    @Test
    void answersAboutPartners() {
        Long id = partners.handle(new RegisterPartnerCommand("Ana Perez", "Engineer")).success().orElseThrow().getId();

        assertThat(facade.existsPartnerById(id)).isTrue();
        assertThat(facade.existsPartnerById(999_999L)).isFalse();
    }
}
