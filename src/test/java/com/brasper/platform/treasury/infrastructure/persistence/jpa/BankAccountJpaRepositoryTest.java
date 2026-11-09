package com.brasper.platform.treasury.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import com.brasper.platform.treasury.domain.model.aggregates.BankAccount;
import com.brasper.platform.treasury.domain.model.commands.OpenBankAccountCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterCashMovementCommand;
import com.brasper.platform.treasury.domain.model.queries.GetCashMovementsByAccountAndPeriodQuery;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountNumber;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountType;
import com.brasper.platform.treasury.domain.model.valueobjects.Bank;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementDirection;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementSource;
import com.brasper.platform.treasury.domain.repositories.BankAccountRepository;
import com.brasper.platform.treasury.domain.repositories.CashMovementRepository;
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
    @Autowired
    private CashMovementRepository movements;

    @Test
    void roundTripsMergedMovementsAndFiltersByAccountAndInclusivePeriod() {
        Long id = accounts.save(account(Bank.BCP, AccountType.CHECKING, "movements")).getId();
        Long otherId = accounts.save(account(Bank.BBVA, AccountType.CHECKING, "other")).getId();
        em.flush();
        em.clear();
        BankAccount found = accounts.findById(id).orElseThrow();
        Period period = new Period(2026, 1);
        Money amount = new Money(new BigDecimal("10.25"), Currency.USD);
        found.registerMovement(new RegisterCashMovementCommand(id, period.startDate(), MovementDirection.INFLOW,
                amount, " collection ", MovementSource.INVOICE_PAYMENT, 9999L));
        found.registerMovement(new RegisterCashMovementCommand(id, period.endDate(), MovementDirection.OUTFLOW,
                amount, null, MovementSource.ADJUSTMENT, null));
        found.registerMovement(new RegisterCashMovementCommand(id, period.next().startDate(), MovementDirection.INFLOW,
                amount, null, MovementSource.ADJUSTMENT, null));
        BankAccount saved = accounts.save(found);
        em.flush();
        Long movementId = saved.getMovements().getFirst().getId();
        assertThat(movementId).isNotNull();
        em.clear();
        BankAccount reloaded = accounts.findById(id).orElseThrow();
        assertThat(reloaded.getMovements()).hasSize(3);
        assertThat(reloaded.currentBalance()).isEqualTo(reloaded.getOpeningBalance().add(amount));
        var query = new GetCashMovementsByAccountAndPeriodQuery(id, period);
        var results = movements.findAllByBankAccountIdAndOccurredOnBetween(
                query.bankAccountId(), query.period().startDate(), query.period().endDate());
        assertThat(results).hasSize(2);
        var movement = results.stream().filter(item -> item.getId().equals(movementId)).findFirst().orElseThrow();
        assertThat(movement).extracting("occurredOn", "direction", "amount", "concept", "source", "sourceId")
                .containsExactly(period.startDate(), MovementDirection.INFLOW, amount, "collection",
                        MovementSource.INVOICE_PAYMENT, 9999L);
        assertThat(movements.findAllByBankAccountIdAndOccurredOnBetween(otherId, period.startDate(), period.endDate()))
                .isEmpty();
    }

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
