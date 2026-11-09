package com.brasper.platform.treasury.application.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Percentage;
import com.brasper.platform.treasury.domain.model.aggregates.BankAccount;
import com.brasper.platform.treasury.domain.model.aggregates.Loan;
import com.brasper.platform.treasury.domain.model.commands.OpenBankAccountCommand;
import com.brasper.platform.treasury.domain.model.commands.PayLoanInstallmentCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterLoanCommand;
import com.brasper.platform.treasury.domain.model.commands.ScheduleLoanInstallmentCommand;
import com.brasper.platform.treasury.domain.model.entities.CashMovement;
import com.brasper.platform.treasury.domain.model.entities.LoanInstallment;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountNumber;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountType;
import com.brasper.platform.treasury.domain.model.valueobjects.Bank;
import com.brasper.platform.treasury.domain.model.valueobjects.LoanType;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementDirection;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementSource;
import com.brasper.platform.treasury.domain.repositories.BankAccountRepository;
import com.brasper.platform.treasury.domain.repositories.LoanRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

class LoanCommandServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 6);
    private final LoanRepository loans = mock(LoanRepository.class);
    private final BankAccountRepository accounts = mock(BankAccountRepository.class);
    private final LoanCommandService service = new LoanCommandService(loans, accounts, mock(ApplicationEventPublisher.class));

    private static Money pen(String amount) {
        return new Money(new BigDecimal(amount), Currency.PEN);
    }

    private static RegisterLoanCommand registration(LoanType type) {
        return new RegisterLoanCommand(type, "Ana Perez", pen("100"),
                Percentage.ofPercent(BigDecimal.TEN), DAY, 5L);
    }

    private BankAccount account(Currency currency) {
        BankAccount account = new BankAccount(new OpenBankAccountCommand(Bank.BCP, new AccountNumber("001-1"),
                AccountType.CHECKING, currency, Money.zero(currency), DAY));
        ReflectionTestUtils.setField(account, "id", 5L);
        when(accounts.findById(5L)).thenReturn(Optional.of(account));
        return account;
    }

    private Loan loan(LoanType type) {
        Loan loan = new Loan(registration(type));
        ReflectionTestUtils.setField(loan, "id", 1L);
        when(loans.findById(1L)).thenReturn(Optional.of(loan));
        return loan;
    }

    private static LoanInstallment installment(Loan loan) {
        LoanInstallment installment = loan.scheduleInstallment(
                new ScheduleLoanInstallmentCommand(1L, DAY, pen("60"), pen("10")));
        ReflectionTestUtils.setField(installment, "id", 2L);
        return installment;
    }

    private void assertDisbursement(LoanType type, MovementDirection direction) {
        BankAccount account = account(Currency.PEN);
        Loan saved = new Loan(registration(type));
        ReflectionTestUtils.setField(saved, "id", 1L);
        when(loans.save(any(Loan.class))).thenReturn(saved);

        assertThat(service.handle(registration(type)).success()).contains(saved);

        assertThat(account.getMovements()).hasSize(1);
        CashMovement movement = account.getMovements().getFirst();
        assertThat(movement.getDirection()).isEqualTo(direction);
        assertThat(movement.getAmount()).isEqualTo(pen("100"));
        assertThat(movement.getSource()).isEqualTo(MovementSource.LOAN_DISBURSEMENT);
        assertThat(movement.getSourceId()).isEqualTo(saved.getId());
        assertThat(movement.getConcept()).isEqualTo("Loan disbursement: Ana Perez");
        assertThat(movement.getOccurredOn()).isEqualTo(DAY);
        var order = inOrder(loans, accounts);
        order.verify(loans).save(any(Loan.class));
        order.verify(accounts).save(account);
    }

    @Test
    void registersReceivedLoanAsInflow() {
        assertDisbursement(LoanType.RECEIVED, MovementDirection.INFLOW);
    }

    @Test
    void registersGrantedLoanAsOutflow() {
        assertDisbursement(LoanType.GRANTED, MovementDirection.OUTFLOW);
    }

    @Test
    void rejectsRegistrationForMissingAccountOrCurrencyMismatch() {
        assertThat(service.handle(registration(LoanType.RECEIVED)).failure())
                .contains(LoanCommandFailure.LOAN_ACCOUNT_NOT_FOUND);
        BankAccount account = account(Currency.USD);
        assertThat(service.handle(registration(LoanType.RECEIVED)).failure())
                .contains(LoanCommandFailure.LOAN_CURRENCY_MISMATCH);
        assertThat(account.getMovements()).isEmpty();
        verify(loans, never()).save(any());
        verify(accounts, never()).save(any());
    }

    @Test
    void schedulesInstallmentsUpToThePrincipal() {
        Loan loan = loan(LoanType.RECEIVED);
        installment(loan);
        when(loans.save(loan)).thenReturn(loan);

        assertThat(service.handle(new ScheduleLoanInstallmentCommand(1L, DAY, pen("40"), pen("10")))
                .success()).contains(loan);
        assertThat(loan.getInstallments()).hasSize(2);
        assertThat(loan.getInstallments().getLast().getPrincipal()).isEqualTo(pen("40"));
        verify(loans).save(loan);
        verify(accounts, never()).save(any());
    }

    @Test
    void rejectsExcessPrincipalBeforeAddingAnInstallment() {
        Loan loan = loan(LoanType.RECEIVED);
        installment(loan);

        assertThat(service.handle(new ScheduleLoanInstallmentCommand(1L, DAY, pen("40.01"), pen("0")))
                .failure()).contains(LoanCommandFailure.INSTALLMENTS_EXCEED_PRINCIPAL);
        assertThat(loan.getInstallments()).hasSize(1);
        verify(loans, never()).save(any());
        verify(accounts, never()).save(any());
    }

    private void assertPayment(LoanType type, MovementDirection direction) {
        Loan loan = loan(type);
        LoanInstallment installment = installment(loan);
        BankAccount account = account(Currency.PEN);
        when(loans.save(loan)).thenReturn(loan);

        assertThat(service.handle(new PayLoanInstallmentCommand(1L, 2L, DAY.plusDays(1), 5L))
                .success()).contains(loan);

        assertThat(installment.isPaid()).isTrue();
        assertThat(account.getMovements()).hasSize(1);
        CashMovement movement = account.getMovements().getFirst();
        assertThat(movement.getDirection()).isEqualTo(direction);
        assertThat(movement.getAmount()).isEqualTo(pen("70"));
        assertThat(movement.getSource()).isEqualTo(MovementSource.LOAN_INSTALLMENT);
        assertThat(movement.getSourceId()).isEqualTo(2L);
        assertThat(movement.getConcept()).isEqualTo("Loan installment: Ana Perez");
        assertThat(movement.getOccurredOn()).isEqualTo(DAY.plusDays(1));
        var order = inOrder(loans, accounts);
        order.verify(loans).save(loan);
        order.verify(accounts).save(account);
    }

    @Test
    void paysReceivedLoanInstallmentAsOutflowOfPrincipalAndInterest() {
        assertPayment(LoanType.RECEIVED, MovementDirection.OUTFLOW);
    }

    @Test
    void paysGrantedLoanInstallmentAsInflowOfPrincipalAndInterest() {
        assertPayment(LoanType.GRANTED, MovementDirection.INFLOW);
    }

    @Test
    void rejectsMissingLoansOrInstallments() {
        assertThat(service.handle(new ScheduleLoanInstallmentCommand(1L, DAY, pen("10"), pen("0")))
                .failure()).contains(LoanCommandFailure.LOAN_NOT_FOUND);
        assertThat(service.handle(new PayLoanInstallmentCommand(1L, 2L, DAY, 5L)).failure())
                .contains(LoanCommandFailure.LOAN_NOT_FOUND);
        installment(loan(LoanType.RECEIVED));
        assertThat(service.handle(new PayLoanInstallmentCommand(1L, 3L, DAY, 5L)).failure())
                .contains(LoanCommandFailure.INSTALLMENT_NOT_FOUND);
        verify(loans, never()).save(any());
        verify(accounts, never()).save(any());
    }

    @Test
    void rejectsAlreadyPaidInstallment() {
        LoanInstallment installment = installment(loan(LoanType.RECEIVED));
        installment.pay(new PayLoanInstallmentCommand(1L, 2L, DAY, 5L));

        assertThat(service.handle(new PayLoanInstallmentCommand(1L, 2L, DAY, 5L)).failure())
                .contains(LoanCommandFailure.INSTALLMENT_ALREADY_PAID);
        verify(loans, never()).save(any());
        verify(accounts, never()).save(any());
    }

    @Test
    void rejectsPaymentForMissingAccountOrCurrencyMismatchWithoutPaying() {
        LoanInstallment installment = installment(loan(LoanType.RECEIVED));
        PayLoanInstallmentCommand command = new PayLoanInstallmentCommand(1L, 2L, DAY, 5L);

        assertThat(service.handle(command).failure()).contains(LoanCommandFailure.LOAN_ACCOUNT_NOT_FOUND);
        BankAccount account = account(Currency.USD);
        assertThat(service.handle(command).failure()).contains(LoanCommandFailure.LOAN_CURRENCY_MISMATCH);
        assertThat(installment.isPaid()).isFalse();
        assertThat(account.getMovements()).isEmpty();
        verify(loans, never()).save(any());
        verify(accounts, never()).save(any());
    }
}
