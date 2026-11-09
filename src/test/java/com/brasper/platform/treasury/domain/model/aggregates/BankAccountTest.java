package com.brasper.platform.treasury.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.treasury.domain.model.commands.OpenBankAccountCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterCashMovementCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountNumber;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountType;
import com.brasper.platform.treasury.domain.model.valueobjects.Bank;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementDirection;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BankAccountTest {

    private static final AccountNumber NUMBER = new AccountNumber("001-123");
    private static final LocalDate OPENED_ON = LocalDate.of(2026, 1, 1);

    @Test
    void computesBalanceFromOpeningBalanceAndSignedMovements() {
        BankAccount account = new BankAccount(new OpenBankAccountCommand(Bank.BCP, NUMBER,
                AccountType.CHECKING, Currency.PEN, new Money(new BigDecimal("100"), Currency.PEN), OPENED_ON));
        assertThat(account.currentBalance()).isEqualTo(account.getOpeningBalance());
        account.registerMovement(new RegisterCashMovementCommand(null, OPENED_ON, MovementDirection.INFLOW,
                new Money(new BigDecimal("25.50"), Currency.PEN), " deposit ", MovementSource.ADJUSTMENT, null));
        account.registerMovement(new RegisterCashMovementCommand(null, OPENED_ON, MovementDirection.OUTFLOW,
                new Money(new BigDecimal("150"), Currency.PEN), null, MovementSource.EXPENSE, 9999L));
        assertThat(account.currentBalance()).isEqualTo(new Money(new BigDecimal("-24.50"), Currency.PEN));
        assertThat(account.getMovements()).hasSize(2);
        assertThat(account.getMovements().getFirst().getConcept()).isEqualTo("deposit");
    }

    @Test
    void rejectsInvalidMovementsWithoutChangingTheBalance() {
        BankAccount account = new BankAccount(new OpenBankAccountCommand(Bank.BCP, NUMBER,
                AccountType.CHECKING, Currency.PEN, Money.zero(Currency.PEN), OPENED_ON));
        for (Money invalid : new Money[]{Money.zero(Currency.PEN), new Money(new BigDecimal("-1"), Currency.PEN),
                new Money(BigDecimal.ONE, Currency.USD), new Money(new BigDecimal("1000000000000"), Currency.PEN)}) {
            assertThatThrownBy(() -> account.registerMovement(new RegisterCashMovementCommand(null, OPENED_ON,
                    MovementDirection.INFLOW, invalid, null, MovementSource.ADJUSTMENT, null)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> account.registerMovement(new RegisterCashMovementCommand(999L, OPENED_ON,
                MovementDirection.INFLOW, new Money(BigDecimal.ONE, Currency.PEN), null, MovementSource.ADJUSTMENT, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(account.getMovements()).isEmpty();
        assertThat(account.currentBalance()).isEqualTo(Money.zero(Currency.PEN));
    }

    @Test
    void rejectsMissingRequiredMovementFieldsAndOversizedConcepts() {
        BankAccount account = new BankAccount(new OpenBankAccountCommand(Bank.BCP, NUMBER,
                AccountType.CHECKING, Currency.PEN, Money.zero(Currency.PEN), OPENED_ON));
        Money amount = new Money(BigDecimal.ONE, Currency.PEN);
        for (RegisterCashMovementCommand command : new RegisterCashMovementCommand[]{
                new RegisterCashMovementCommand(null, null, MovementDirection.INFLOW, amount, null, MovementSource.ADJUSTMENT, null),
                new RegisterCashMovementCommand(null, OPENED_ON, null, amount, null, MovementSource.ADJUSTMENT, null),
                new RegisterCashMovementCommand(null, OPENED_ON, MovementDirection.INFLOW, null, null, MovementSource.ADJUSTMENT, null),
                new RegisterCashMovementCommand(null, OPENED_ON, MovementDirection.INFLOW, amount, null, null, null)}) {
            assertThatThrownBy(() -> account.registerMovement(command)).isInstanceOf(NullPointerException.class);
        }
        assertThatThrownBy(() -> account.registerMovement(new RegisterCashMovementCommand(null, OPENED_ON,
                MovementDirection.INFLOW, amount, "x".repeat(501), MovementSource.ADJUSTMENT, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(account.getMovements()).isEmpty();
    }

    @Test
    void opensEachAccountTypeWithZeroOrSignedOpeningBalance() {
        for (AccountType type : AccountType.values()) {
            for (String amount : new String[]{"0", "123.456", "-10", "999999999999.99"}) {
                Money balance = new Money(new BigDecimal(amount), Currency.PEN);
                BankAccount account = new BankAccount(new OpenBankAccountCommand(
                        Bank.BCP, NUMBER, type, Currency.PEN, balance, OPENED_ON));
                assertThat(account.getBank()).isEqualTo(Bank.BCP);
                assertThat(account.getAccountNumber()).isEqualTo(NUMBER);
                assertThat(account.getType()).isEqualTo(type);
                assertThat(account.getCurrency()).isEqualTo(Currency.PEN);
                assertThat(account.getOpeningBalance()).isEqualTo(balance);
                assertThat(account.getOpenedOn()).isEqualTo(OPENED_ON);
            }
        }
    }

    @Test
    void validatesAndTrimsAccountNumbersWithoutAssumingABankSpecificFormat() {
        assertThat(new AccountNumber("  001-123  ")).isEqualTo(NUMBER);
        assertThat(new AccountNumber("x".repeat(50)).value()).hasSize(50);
        for (String invalid : new String[]{"", " ", "x".repeat(51)}) {
            assertThatThrownBy(() -> new AccountNumber(invalid)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> new AccountNumber(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsCurrencyMismatchAndBalancesTooLargeForStorage() {
        assertThatThrownBy(() -> new BankAccount(new OpenBankAccountCommand(
                Bank.BCP, NUMBER, AccountType.CHECKING, Currency.USD, Money.zero(Currency.PEN), OPENED_ON)))
                .isInstanceOf(IllegalArgumentException.class);
        for (String amount : new String[]{"1000000000000", "-1000000000000", "999999999999.999"}) {
            assertThatThrownBy(() -> new BankAccount(new OpenBankAccountCommand(Bank.BCP, NUMBER,
                    AccountType.CHECKING, Currency.PEN, new Money(new BigDecimal(amount), Currency.PEN), OPENED_ON)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void rejectsEveryMissingRequiredField() {
        Money zero = Money.zero(Currency.PEN);
        for (OpenBankAccountCommand command : new OpenBankAccountCommand[]{
                new OpenBankAccountCommand(null, NUMBER, AccountType.CHECKING, Currency.PEN, zero, OPENED_ON),
                new OpenBankAccountCommand(Bank.BCP, null, AccountType.CHECKING, Currency.PEN, zero, OPENED_ON),
                new OpenBankAccountCommand(Bank.BCP, NUMBER, null, Currency.PEN, zero, OPENED_ON),
                new OpenBankAccountCommand(Bank.BCP, NUMBER, AccountType.CHECKING, null, zero, OPENED_ON),
                new OpenBankAccountCommand(Bank.BCP, NUMBER, AccountType.CHECKING, Currency.PEN, null, OPENED_ON),
                new OpenBankAccountCommand(Bank.BCP, NUMBER, AccountType.CHECKING, Currency.PEN, zero, null)}) {
            assertThatThrownBy(() -> new BankAccount(command)).isInstanceOf(NullPointerException.class);
        }
    }
}
