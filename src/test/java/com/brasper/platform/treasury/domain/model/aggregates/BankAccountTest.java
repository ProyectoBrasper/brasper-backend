package com.brasper.platform.treasury.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.treasury.domain.model.commands.OpenBankAccountCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountNumber;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountType;
import com.brasper.platform.treasury.domain.model.valueobjects.Bank;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BankAccountTest {

    private static final AccountNumber NUMBER = new AccountNumber("001-123");
    private static final LocalDate OPENED_ON = LocalDate.of(2026, 1, 1);

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
