package com.brasper.platform.treasury.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.treasury.domain.model.aggregates.BankAccount;
import com.brasper.platform.treasury.domain.model.commands.OpenBankAccountCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountNumber;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountType;
import com.brasper.platform.treasury.domain.model.valueobjects.Bank;
import com.brasper.platform.treasury.domain.repositories.BankAccountRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class BankAccountJpaRepositoryTest {

    @Autowired
    private BankAccountRepository accounts;
    @Autowired
    private EntityManager em;

    private static BankAccount account(Bank bank, AccountType type, String number) {
        return new BankAccount(new OpenBankAccountCommand(bank, new AccountNumber(number), type,
                Currency.USD, new Money(new BigDecimal("123.45"), Currency.USD), LocalDate.of(2026, 1, 1)));
    }

    @Test
    void roundTripsEveryFieldAndFindsAccountsByNumber() {
        for (Bank bank : Bank.values()) {
            for (AccountType type : AccountType.values()) {
                AccountNumber number = new AccountNumber(bank + "-" + type);
                Long id = accounts.save(account(bank, type, number.value())).getId();
                em.flush();
                em.clear();
                BankAccount found = accounts.findById(id).orElseThrow();
                assertThat(found.getBank()).isEqualTo(bank);
                assertThat(found.getType()).isEqualTo(type);
                assertThat(found.getAccountNumber()).isEqualTo(number);
                assertThat(found.getCurrency()).isEqualTo(Currency.USD);
                assertThat(found.getOpeningBalance()).isEqualTo(new Money(new BigDecimal("123.45"), Currency.USD));
                assertThat(found.getOpenedOn()).isEqualTo(LocalDate.of(2026, 1, 1));
                assertThat(found.getCreatedAt()).isNotNull();
                assertThat(found.getUpdatedAt()).isEqualTo(found.getCreatedAt());
                assertThat(accounts.existsByAccountNumber(number)).isTrue();
                assertThat(accounts.findByAccountNumber(number)).contains(found);
            }
        }
        assertThat(accounts.findAll()).hasSize(12);
        assertThat(accounts.findAllByType(AccountType.DETRACTIONS)).hasSize(4);
        assertThat(accounts.existsByAccountNumber(new AccountNumber("missing"))).isFalse();
    }

    @Test
    void databaseRejectsDuplicateAccountNumbersEvenAcrossBanks() {
        accounts.save(account(Bank.BCP, AccountType.CHECKING, "001-123"));
        em.flush();
        em.clear();
        assertThatThrownBy(() -> {
            accounts.save(account(Bank.BBVA, AccountType.CHECKING, " 001-123 "));
            em.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
