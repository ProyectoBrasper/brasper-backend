package com.brasper.platform.treasury.application.commandservices;

import com.brasper.platform.shared.application.result.Result;
import com.brasper.platform.shared.domain.model.valueobjects.PaymentStatus;
import com.brasper.platform.treasury.domain.model.aggregates.BankAccount;
import com.brasper.platform.treasury.domain.events.PayablePaidEvent;
import com.brasper.platform.treasury.domain.events.PayableRegisteredEvent;
import com.brasper.platform.treasury.domain.events.PayableSettledEvent;
import com.brasper.platform.treasury.domain.model.aggregates.Payable;
import com.brasper.platform.treasury.domain.model.commands.CreatePayableCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterCashMovementCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterPayablePaymentCommand;
import com.brasper.platform.treasury.domain.model.commands.ReschedulePayableCommand;
import com.brasper.platform.treasury.domain.model.entities.PayablePayment;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementDirection;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementSource;
import com.brasper.platform.treasury.domain.repositories.BankAccountRepository;
import com.brasper.platform.treasury.domain.repositories.PayableRepository;
import java.time.LocalDate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayableCommandService {

    private final PayableRepository payables;
    private final BankAccountRepository accounts;
    private final ApplicationEventPublisher events;

    public PayableCommandService(PayableRepository payables, BankAccountRepository accounts,
            ApplicationEventPublisher events) {
        this.payables = payables;
        this.accounts = accounts;
        this.events = events;
    }

    @Transactional
    public Result<Payable, PayableCommandFailure> handle(CreatePayableCommand command) {
        Payable saved = payables.save(new Payable(command));
        events.publishEvent(new PayableRegisteredEvent(saved.getId(), saved.getExpenseId(), saved.getType(),
                saved.getProjectId(), saved.getTotalAmount(), LocalDate.now()));
        return Result.success(saved);
    }

    @Transactional
    public Result<Payable, PayableCommandFailure> handle(RegisterPayablePaymentCommand command) {
        Payable payable = payables.findById(command.payableId()).orElse(null);
        if (payable == null) {
            return Result.failure(PayableCommandFailure.PAYABLE_NOT_FOUND);
        }
        if (payable.getStatus() == PaymentStatus.PAID) {
            return Result.failure(PayableCommandFailure.PAYABLE_ALREADY_PAID);
        }
        if (command.amount() == null || !command.amount().isPositive()) {
            return Result.failure(PayableCommandFailure.INVALID_PAYMENT_AMOUNT);
        }
        BankAccount account = accounts.findById(command.bankAccountId()).orElse(null);
        if (account == null) {
            return Result.failure(PayableCommandFailure.PAYMENT_ACCOUNT_NOT_FOUND);
        }
        if (command.amount().currency() != payable.getTotalAmount().currency()
                || command.amount().currency() != account.getCurrency()) {
            return Result.failure(PayableCommandFailure.PAYMENT_CURRENCY_MISMATCH);
        }
        if (command.amount().amount().compareTo(payable.balance().amount()) > 0) {
            return Result.failure(PayableCommandFailure.PAYMENT_EXCEEDS_BALANCE);
        }
        payable.registerPayment(command);
        Payable saved = payables.save(payable);
        // Merge copies child entities; the generated payment id belongs to the saved aggregate.
        PayablePayment payment = saved.getPayments().getLast();
        account.registerMovement(new RegisterCashMovementCommand(account.getId(), command.paidOn(),
                MovementDirection.OUTFLOW, command.amount(), "Payable payment: " + saved.getConcept(),
                MovementSource.PAYABLE_PAYMENT, payment.getId()));
        accounts.save(account);
        events.publishEvent(new PayablePaidEvent(payment.getId(), saved.getId(), command.paidOn(), command.amount(),
                account.getId()));
        if (saved.getStatus() == PaymentStatus.PAID && saved.getExpenseId() != null) {
            events.publishEvent(new PayableSettledEvent(saved.getId(), saved.getExpenseId(), command.paidOn()));
        }
        return Result.success(saved);
    }

    @Transactional
    public Result<Payable, PayableCommandFailure> handle(ReschedulePayableCommand command) {
        Payable payable = payables.findById(command.payableId()).orElse(null);
        if (payable == null) {
            return Result.failure(PayableCommandFailure.PAYABLE_NOT_FOUND);
        }
        if (payable.getStatus() == PaymentStatus.PAID) {
            return Result.failure(PayableCommandFailure.PAYABLE_ALREADY_PAID);
        }
        payable.reschedule(command.scheduledWeek());
        return Result.success(payables.save(payable));
    }
}
