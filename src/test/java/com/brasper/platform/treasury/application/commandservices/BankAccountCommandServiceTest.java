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
import com.brasper.platform.treasury.domain.model.commands.OpenBankAccountCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterCashMovementCommand;
import com.brasper.platform.treasury.domain.model.commands.TransferFundsCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountNumber;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountType;
import com.brasper.platform.treasury.domain.model.valueobjects.Bank;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementDirection;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementSource;
import com.brasper.platform.treasury.domain.repositories.BankAccountRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class BankAccountCommandServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 6);

    private final BankAccountRepository accounts = mock(BankAccountRepository.class);
    private final BankAccountCommandService service = new BankAccountCommandService(accounts);

    private static Money pen(String amount) {
        return new Money(new BigDecimal(amount), Currency.PEN);
    }

    private static BankAccount account(String number, Currency currency, String opening) {
        return new BankAccount(new OpenBankAccountCommand(Bank.BCP, new AccountNumber(number), AccountType.CHECKING,
                currency, new Money(new BigDecimal(opening), currency), DAY));
    }

    private void saveReturnsArgument() {
        when(accounts.save(any(BankAccount.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void opensAnAccountAndRejectsADuplicateNumber() {
        saveReturnsArgument();
        OpenBankAccountCommand command = new OpenBankAccountCommand(Bank.BCP, new AccountNumber("001-1"),
                AccountType.CHECKING, Currency.PEN, pen("100"), DAY);
        when(accounts.existsByAccountNumber(command.accountNumber())).thenReturn(false, true);

        assertThat(service.handle(command).success().orElseThrow().getOpeningBalance()).isEqualTo(pen("100"));
        assertThat(service.handle(command).failure()).contains(BankAccountCommandFailure.DUPLICATE_ACCOUNT_NUMBER);
    }

    @Test
    void rejectsAnAccountInUsd() {
        OpenBankAccountCommand usd = new OpenBankAccountCommand(Bank.BCP, new AccountNumber("001-1"),
                AccountType.CHECKING, Currency.USD, new Money(BigDecimal.TEN, Currency.USD), DAY);

        assertThat(service.handle(usd).failure()).contains(BankAccountCommandFailure.UNSUPPORTED_CURRENCY);
        verify(accounts, never()).save(any());
    }

    @Test
    void registersAMovementOnAnExistingAccount() {
        saveReturnsArgument();
        BankAccount existing = account("001-1", Currency.PEN, "100");
        ReflectionTestUtils.setField(existing, "id", 1L);
        when(accounts.findById(1L)).thenReturn(Optional.of(existing));

        BankAccount result = service.handle(new RegisterCashMovementCommand(1L, DAY, MovementDirection.INFLOW,
                pen("50"), "adjustment", MovementSource.ADJUSTMENT, null)).success().orElseThrow();

        assertThat(result.currentBalance()).isEqualTo(pen("150"));
    }

    @Test
    void rejectsMovementsOnMissingAccountsOrInAnotherCurrency() {
        when(accounts.findById(1L)).thenReturn(Optional.empty());
        when(accounts.findById(2L)).thenReturn(Optional.of(account("001-2", Currency.USD, "0")));

        assertThat(service.handle(new RegisterCashMovementCommand(1L, DAY, MovementDirection.INFLOW, pen("1"),
                null, MovementSource.ADJUSTMENT, null)).failure()).contains(BankAccountCommandFailure.BANK_ACCOUNT_NOT_FOUND);
        assertThat(service.handle(new RegisterCashMovementCommand(2L, DAY, MovementDirection.INFLOW, pen("1"),
                null, MovementSource.ADJUSTMENT, null)).failure()).contains(BankAccountCommandFailure.CURRENCY_MISMATCH);
        verify(accounts, never()).save(any());
    }

    @Test
    void transfersFundsAsAnOutflowAndAnInflow() {
        saveReturnsArgument();
        BankAccount from = account("001-1", Currency.PEN, "100");
        BankAccount to = account("001-2", Currency.PEN, "10");
        when(accounts.findById(1L)).thenReturn(Optional.of(from));
        when(accounts.findById(2L)).thenReturn(Optional.of(to));

        BankAccount result = service.handle(new TransferFundsCommand(1L, 2L, DAY, pen("40"), "move")).success().orElseThrow();

        assertThat(result).isSameAs(from);
        assertThat(from.currentBalance()).isEqualTo(pen("60"));
        assertThat(to.currentBalance()).isEqualTo(pen("50"));
        assertThat(from.getMovements().getFirst().getSource()).isEqualTo(MovementSource.TRANSFER);
        verify(accounts).save(to);
    }

    @Test
    void rejectsInvalidTransfersWithoutMovingMoney() {
        BankAccount pen = account("001-1", Currency.PEN, "100");
        BankAccount usd = account("001-2", Currency.USD, "100");
        when(accounts.findById(1L)).thenReturn(Optional.of(pen));
        when(accounts.findById(2L)).thenReturn(Optional.of(usd));
        when(accounts.findById(3L)).thenReturn(Optional.empty());

        assertThat(service.handle(new TransferFundsCommand(1L, 1L, DAY, pen("1"), null)).failure())
                .contains(BankAccountCommandFailure.SAME_ACCOUNT);
        assertThat(service.handle(new TransferFundsCommand(1L, 3L, DAY, pen("1"), null)).failure())
                .contains(BankAccountCommandFailure.BANK_ACCOUNT_NOT_FOUND);
        assertThat(service.handle(new TransferFundsCommand(1L, 2L, DAY, pen("1"), null)).failure())
                .contains(BankAccountCommandFailure.CURRENCY_MISMATCH);
        assertThat(pen.getMovements()).isEmpty();
        verify(accounts, never()).save(any());
    }
}
