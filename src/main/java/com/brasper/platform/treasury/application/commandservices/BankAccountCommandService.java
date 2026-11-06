package com.brasper.platform.treasury.application.commandservices;

import com.brasper.platform.shared.application.result.Result;
import com.brasper.platform.treasury.domain.model.aggregates.BankAccount;
import com.brasper.platform.treasury.domain.model.commands.OpenBankAccountCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterCashMovementCommand;
import com.brasper.platform.treasury.domain.model.commands.TransferFundsCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementDirection;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementSource;
import com.brasper.platform.treasury.domain.repositories.BankAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BankAccountCommandService {

    private final BankAccountRepository accounts;

    public BankAccountCommandService(BankAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Transactional
    public Result<BankAccount, BankAccountCommandFailure> handle(OpenBankAccountCommand command) {
        if (accounts.existsByAccountNumber(command.accountNumber())) {
            return Result.failure(BankAccountCommandFailure.DUPLICATE_ACCOUNT_NUMBER);
        }
        return Result.success(accounts.save(new BankAccount(command)));
    }

    @Transactional
    public Result<BankAccount, BankAccountCommandFailure> handle(RegisterCashMovementCommand command) {
        BankAccount account = accounts.findById(command.bankAccountId()).orElse(null);
        if (account == null) {
            return Result.failure(BankAccountCommandFailure.BANK_ACCOUNT_NOT_FOUND);
        }
        if (command.amount().currency() != account.getCurrency()) {
            return Result.failure(BankAccountCommandFailure.CURRENCY_MISMATCH);
        }
        account.registerMovement(command);
        return Result.success(accounts.save(account));
    }

    /** Moves funds between two accounts; returns the origin account. The counterpart account id is the movements' sourceId. */
    @Transactional
    public Result<BankAccount, BankAccountCommandFailure> handle(TransferFundsCommand command) {
        if (command.fromAccountId().equals(command.toAccountId())) {
            return Result.failure(BankAccountCommandFailure.SAME_ACCOUNT);
        }
        BankAccount from = accounts.findById(command.fromAccountId()).orElse(null);
        BankAccount to = accounts.findById(command.toAccountId()).orElse(null);
        if (from == null || to == null) {
            return Result.failure(BankAccountCommandFailure.BANK_ACCOUNT_NOT_FOUND);
        }
        if (command.amount().currency() != from.getCurrency() || from.getCurrency() != to.getCurrency()) {
            return Result.failure(BankAccountCommandFailure.CURRENCY_MISMATCH);
        }
        from.registerMovement(movement(from, to, MovementDirection.OUTFLOW, command));
        to.registerMovement(movement(to, from, MovementDirection.INFLOW, command));
        accounts.save(to);
        return Result.success(accounts.save(from));
    }

    private static RegisterCashMovementCommand movement(BankAccount account, BankAccount counterpart,
            MovementDirection direction, TransferFundsCommand command) {
        return new RegisterCashMovementCommand(account.getId(), command.transferredOn(), direction,
                command.amount(), command.concept(), MovementSource.TRANSFER, counterpart.getId());
    }
}
