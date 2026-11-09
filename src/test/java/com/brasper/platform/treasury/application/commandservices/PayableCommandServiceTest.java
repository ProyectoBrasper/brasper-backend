package com.brasper.platform.treasury.application.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.PaymentStatus;
import com.brasper.platform.treasury.domain.model.aggregates.BankAccount;
import com.brasper.platform.treasury.domain.model.aggregates.Payable;
import com.brasper.platform.treasury.domain.model.commands.CreatePayableCommand;
import com.brasper.platform.treasury.domain.model.commands.OpenBankAccountCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterPayablePaymentCommand;
import com.brasper.platform.treasury.domain.model.commands.ReschedulePayableCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountNumber;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountType;
import com.brasper.platform.treasury.domain.model.valueobjects.Bank;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementDirection;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementSource;
import com.brasper.platform.treasury.domain.model.valueobjects.PayableType;
import com.brasper.platform.treasury.domain.repositories.BankAccountRepository;
import com.brasper.platform.treasury.domain.repositories.PayableRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

class PayableCommandServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 6);
    private final PayableRepository payables = mock(PayableRepository.class);
    private final BankAccountRepository accounts = mock(BankAccountRepository.class);
    private final PayableCommandService service = new PayableCommandService(payables, accounts, mock(ApplicationEventPublisher.class));

    private static Money pen(String amount) {
        return new Money(new BigDecimal(amount), Currency.PEN);
    }

    private static CreatePayableCommand create() {
        return new CreatePayableCommand("materials", PayableType.SUPPLIER, pen("100"),
                null, null, null, new CashFlowWeek(2026, 11, 6));
    }

    private static RegisterPayablePaymentCommand payment(Money amount) {
        return new RegisterPayablePaymentCommand(1L, DAY, amount, 5L);
    }

    private Payable existingPayable() {
        Payable payable = new Payable(create());
        ReflectionTestUtils.setField(payable, "id", 1L);
        when(payables.findById(1L)).thenReturn(Optional.of(payable));
        return payable;
    }

    private BankAccount existingAccount(Currency currency) {
        BankAccount account = new BankAccount(new OpenBankAccountCommand(Bank.BCP, new AccountNumber("001-1"),
                AccountType.CHECKING, currency, new Money(new BigDecimal("200"), currency), DAY));
        ReflectionTestUtils.setField(account, "id", 5L);
        when(accounts.findById(5L)).thenReturn(Optional.of(account));
        return account;
    }

    private void nothingSaved() {
        verify(payables, never()).save(any());
        verify(accounts, never()).save(any());
    }

    @Test
    void rejectsAPayableInUsd() {
        var usd = new CreatePayableCommand("materials", PayableType.SUPPLIER, new Money(BigDecimal.TEN, Currency.USD),
                null, null, null, new CashFlowWeek(2026, 11, 6));

        assertThat(service.handle(usd).failure()).contains(PayableCommandFailure.UNSUPPORTED_CURRENCY);
        nothingSaved();
    }

    @Test
    void createsAPayable() {
        when(payables.save(any(Payable.class))).thenAnswer(call -> call.getArgument(0));
        Payable result = service.handle(create()).success().orElseThrow();
        assertThat(result.getConcept()).isEqualTo("materials");
        assertThat(result.getTotalAmount()).isEqualTo(pen("100"));
        verify(payables).save(result);
        verify(accounts, never()).save(any());
    }

    @Test
    void partialAndFullPaymentsCreateOutflowsUsingSavedPaymentIds() {
        Payable payable = existingPayable();
        BankAccount account = existingAccount(Currency.PEN);
        when(payables.save(any(Payable.class))).thenAnswer(call -> {
            Payable saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved.getPayments().getLast(), "id", (long) saved.getPayments().size());
            return saved;
        });

        assertThat(service.handle(payment(pen("40"))).success().orElseThrow()).isSameAs(payable);
        assertThat(payable.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
        assertThat(payable.balance()).isEqualTo(pen("60"));
        var movement = account.getMovements().getFirst();
        assertThat(movement.getDirection()).isEqualTo(MovementDirection.OUTFLOW);
        assertThat(movement.getAmount()).isEqualTo(pen("40"));
        assertThat(movement.getOccurredOn()).isEqualTo(DAY);
        assertThat(movement.getConcept()).isEqualTo("Payable payment: materials");
        assertThat(movement.getSource()).isEqualTo(MovementSource.PAYABLE_PAYMENT);
        assertThat(movement.getSourceId()).isEqualTo(1L);
        verify(accounts).save(account);

        assertThat(service.handle(payment(pen("60"))).success().orElseThrow().getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(account.getMovements()).hasSize(2);
        assertThat(account.getMovements().getLast().getSourceId()).isEqualTo(2L);
        assertThat(account.getMovements().getLast().getAmount()).isEqualTo(pen("60"));
        assertThat(account.currentBalance()).isEqualTo(pen("100"));
    }

    @Test
    void usesTheAggregateReturnedByMergeInsteadOfTheOriginalPayment() {
        Payable original = existingPayable();
        BankAccount account = existingAccount(Currency.PEN);
        Payable merged = new Payable(create());
        merged.registerPayment(payment(pen("40")));
        ReflectionTestUtils.setField(merged.getPayments().getLast(), "id", 77L);
        when(payables.save(original)).thenReturn(merged);

        assertThat(service.handle(payment(pen("40"))).success()).contains(merged);
        assertThat(original.getPayments().getLast().getId()).isNull();
        assertThat(account.getMovements().getLast().getSourceId()).isEqualTo(77L);
    }

    @Test
    void rejectsMissingAndPaidPayablesWithoutSaving() {
        when(payables.findById(1L)).thenReturn(Optional.empty());
        assertThat(service.handle(payment(pen("1"))).failure()).contains(PayableCommandFailure.PAYABLE_NOT_FOUND);
        Payable payable = existingPayable();
        payable.registerPayment(payment(pen("100")));
        assertThat(service.handle(payment(pen("1"))).failure()).contains(PayableCommandFailure.PAYABLE_ALREADY_PAID);
        nothingSaved();
    }

    @Test
    void rejectsInvalidAmountsMissingAccountsCurrenciesAndOverpaymentsBeforeMutation() {
        Payable payable = existingPayable();
        for (Money amount : new Money[]{null, pen("0"), pen("-1")}) {
            assertThat(service.handle(payment(amount)).failure()).contains(PayableCommandFailure.INVALID_PAYMENT_AMOUNT);
        }
        when(accounts.findById(5L)).thenReturn(Optional.empty());
        assertThat(service.handle(payment(pen("1"))).failure()).contains(PayableCommandFailure.PAYMENT_ACCOUNT_NOT_FOUND);
        BankAccount usd = existingAccount(Currency.USD);
        assertThat(service.handle(payment(pen("1"))).failure()).contains(PayableCommandFailure.PAYMENT_CURRENCY_MISMATCH);
        assertThat(service.handle(payment(new Money(BigDecimal.ONE, Currency.USD))).failure())
                .contains(PayableCommandFailure.PAYMENT_CURRENCY_MISMATCH);
        BankAccount pen = existingAccount(Currency.PEN);
        payable.registerPayment(payment(pen("40")));
        assertThat(service.handle(payment(pen("60.01"))).failure()).contains(PayableCommandFailure.PAYMENT_EXCEEDS_BALANCE);
        assertThat(payable.getPayments()).hasSize(1);
        assertThat(payable.balance()).isEqualTo(pen("60"));
        assertThat(usd.getMovements()).isEmpty();
        assertThat(pen.getMovements()).isEmpty();
        nothingSaved();
    }

    @Test
    void reschedulesAnUnpaidPayable() {
        Payable payable = existingPayable();
        when(payables.save(payable)).thenReturn(payable);
        CashFlowWeek week = new CashFlowWeek(2027, 1, 2);
        assertThat(service.handle(new ReschedulePayableCommand(1L, week)).success()).contains(payable);
        assertThat(payable.getScheduledWeek()).isEqualTo(week);
        verify(payables).save(payable);
        verify(accounts, never()).save(any());
    }

    @Test
    void rejectsReschedulingMissingOrPaidPayables() {
        ReschedulePayableCommand command = new ReschedulePayableCommand(1L, new CashFlowWeek(2027, 1, 2));
        when(payables.findById(1L)).thenReturn(Optional.empty());
        assertThat(service.handle(command).failure()).contains(PayableCommandFailure.PAYABLE_NOT_FOUND);
        Payable payable = existingPayable();
        payable.registerPayment(payment(pen("100")));
        assertThat(service.handle(command).failure()).contains(PayableCommandFailure.PAYABLE_ALREADY_PAID);
        assertThat(payable.getScheduledWeek()).isEqualTo(create().scheduledWeek());
        nothingSaved();
    }
}
