package com.brasper.platform.treasury.application.commandservices;

import com.brasper.platform.shared.application.result.Result;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.treasury.domain.model.aggregates.BankAccount;
import com.brasper.platform.treasury.domain.model.aggregates.Loan;
import com.brasper.platform.treasury.domain.model.commands.PayLoanInstallmentCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterCashMovementCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterLoanCommand;
import com.brasper.platform.treasury.domain.model.commands.ScheduleLoanInstallmentCommand;
import com.brasper.platform.treasury.domain.model.entities.LoanInstallment;
import com.brasper.platform.treasury.domain.model.valueobjects.LoanType;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementDirection;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementSource;
import com.brasper.platform.treasury.domain.repositories.BankAccountRepository;
import com.brasper.platform.treasury.domain.repositories.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanCommandService {

    private final LoanRepository loans;
    private final BankAccountRepository accounts;

    public LoanCommandService(LoanRepository loans, BankAccountRepository accounts) {
        this.loans = loans;
        this.accounts = accounts;
    }

    @Transactional
    public Result<Loan, LoanCommandFailure> handle(RegisterLoanCommand command) {
        BankAccount account = accounts.findById(command.bankAccountId()).orElse(null);
        if (account == null) {
            return Result.failure(LoanCommandFailure.LOAN_ACCOUNT_NOT_FOUND);
        }
        if (command.principal().currency() != account.getCurrency()) {
            return Result.failure(LoanCommandFailure.LOAN_CURRENCY_MISMATCH);
        }
        Loan loan = loans.save(new Loan(command));
        MovementDirection direction = loan.getType() == LoanType.RECEIVED
                ? MovementDirection.INFLOW : MovementDirection.OUTFLOW;
        account.registerMovement(new RegisterCashMovementCommand(account.getId(), loan.getDisbursedOn(), direction,
                loan.getPrincipal(), "Loan disbursement: " + loan.getCounterparty(),
                MovementSource.LOAN_DISBURSEMENT, loan.getId()));
        accounts.save(account);
        return Result.success(loan);
    }

    @Transactional
    public Result<Loan, LoanCommandFailure> handle(ScheduleLoanInstallmentCommand command) {
        Loan loan = loans.findById(command.loanId()).orElse(null);
        if (loan == null) {
            return Result.failure(LoanCommandFailure.LOAN_NOT_FOUND);
        }
        Money scheduled = loan.getInstallments().stream().map(LoanInstallment::getPrincipal)
                .reduce(Money.zero(loan.getPrincipal().currency()), Money::add);
        if (scheduled.add(command.principal()).amount().compareTo(loan.getPrincipal().amount()) > 0) {
            return Result.failure(LoanCommandFailure.INSTALLMENTS_EXCEED_PRINCIPAL);
        }
        loan.scheduleInstallment(command);
        return Result.success(loans.save(loan));
    }

    @Transactional
    public Result<Loan, LoanCommandFailure> handle(PayLoanInstallmentCommand command) {
        Loan loan = loans.findById(command.loanId()).orElse(null);
        if (loan == null) {
            return Result.failure(LoanCommandFailure.LOAN_NOT_FOUND);
        }
        LoanInstallment installment = loan.getInstallments().stream()
                .filter(item -> command.installmentId() != null && command.installmentId().equals(item.getId()))
                .findFirst().orElse(null);
        if (installment == null) {
            return Result.failure(LoanCommandFailure.INSTALLMENT_NOT_FOUND);
        }
        if (installment.isPaid()) {
            return Result.failure(LoanCommandFailure.INSTALLMENT_ALREADY_PAID);
        }
        BankAccount account = accounts.findById(command.bankAccountId()).orElse(null);
        if (account == null) {
            return Result.failure(LoanCommandFailure.LOAN_ACCOUNT_NOT_FOUND);
        }
        if (installment.total().currency() != account.getCurrency()) {
            return Result.failure(LoanCommandFailure.LOAN_CURRENCY_MISMATCH);
        }
        Money total = installment.total();
        loan.payInstallment(command);
        loan = loans.save(loan);
        MovementDirection direction = loan.getType() == LoanType.RECEIVED
                ? MovementDirection.OUTFLOW : MovementDirection.INFLOW;
        account.registerMovement(new RegisterCashMovementCommand(account.getId(), command.paidOn(), direction,
                total, "Loan installment: " + loan.getCounterparty(),
                MovementSource.LOAN_INSTALLMENT, command.installmentId()));
        accounts.save(account);
        return Result.success(loan);
    }
}
