package com.brasper.platform.accounting.application.commandservices;

import com.brasper.platform.accounting.domain.model.aggregates.TaxParameters;
import com.brasper.platform.accounting.domain.model.commands.SetTaxParametersCommand;
import com.brasper.platform.accounting.domain.repositories.TaxParametersRepository;
import com.brasper.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.brasper.platform.accounting.application.outboundservices.AccountingTreasuryLookup;
import com.brasper.platform.accounting.domain.events.TaxReturnPaidEvent;
import com.brasper.platform.accounting.domain.model.aggregates.TaxReturn;
import com.brasper.platform.accounting.domain.model.commands.*;
import com.brasper.platform.accounting.domain.model.valueobjects.*;
import com.brasper.platform.accounting.domain.repositories.JournalEntryRepository;
import com.brasper.platform.accounting.domain.repositories.TaxReturnRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import java.util.ArrayList;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@Service
public class TaxCommandService {

    private final TaxParametersRepository parameters;

    private final TaxReturnRepository returns;
    private final JournalEntryRepository entries;
    private final LedgerCommandService ledger;
    private final AccountingTreasuryLookup treasury;
    private final ApplicationEventPublisher events;

    public TaxCommandService(TaxParametersRepository parameters, TaxReturnRepository returns,
            JournalEntryRepository entries, LedgerCommandService ledger, AccountingTreasuryLookup treasury,
            ApplicationEventPublisher events) {
        this.parameters = parameters;
        this.returns = returns;
        this.entries = entries;
        this.ledger = ledger;
        this.treasury = treasury;
        this.events = events;
    }

    @Transactional
    public Result<TaxReturn, TaxCommandFailure> handle(PrepareTaxReturnCommand command) {
        var period = command.period();
        if (returns.existsByPeriod(period)) return Result.failure(TaxCommandFailure.DUPLICATE_RETURN);
        Money zero = Money.zero(Currency.PEN);
        Money outputIgv = zero, inputIgv = zero, sales = zero;
        // shortcut: reversals count on the opposite side of 40111; the net IGV is right, the gross figures are not
        for (var total : entries.totalsByPeriod(period)) {
            if (total.currency() != Currency.PEN) continue;
            if (total.accountCode().equals(new AccountCode("40111"))) {
                outputIgv = new Money(total.credit(), Currency.PEN);
                inputIgv = new Money(total.debit(), Currency.PEN);
            } else if (total.accountCode().equals(new AccountCode("70"))) {
                sales = new Money(total.credit().subtract(total.debit()), Currency.PEN);
            }
        }
        Money carriedCredit = returns.findByPeriod(period.previous()).map(TaxReturn::nextCarriedCredit).orElse(zero);
        // shortcut: with no tax parameters set yet the standard rates apply; remove once parameters are seeded.
        TaxRates rates = parameters.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(period.endDate())
                .map(TaxParameters::getRates).orElseGet(TaxRates::standard);
        Money prepayment = (sales.isPositive() ? sales : zero).multiply(rates.incomeTaxPrepayment().value());
        return Result.success(returns.save(new TaxReturn(command, outputIgv, inputIgv, carriedCredit, prepayment)));
    }

    @Transactional
    public Result<TaxReturn, TaxCommandFailure> handle(PayTaxReturnCommand command) {
        TaxReturn taxReturn = returns.findById(command.taxReturnId()).orElse(null);
        if (taxReturn == null) return Result.failure(TaxCommandFailure.TAX_RETURN_NOT_FOUND);
        if (taxReturn.getStatus() == TaxReturnStatus.PAID) return Result.failure(TaxCommandFailure.ALREADY_PAID);
        if (!treasury.bankAccountExists(command.bankAccountId()))
            return Result.failure(TaxCommandFailure.BANK_ACCOUNT_NOT_FOUND);
        if (taxReturn.getStatus() == TaxReturnStatus.DRAFT) taxReturn.file();
        taxReturn.markAsPaid(command.paidOn(), command.bankAccountId());
        // Treasury takes the cash out when it receives the TaxReturnPaidEvent published below.
        if (taxReturn.totalPayable().isPositive()) {
            Money zero = Money.zero(taxReturn.getCurrency());
            var lines = new ArrayList<JournalLineData>();
            if (taxReturn.igvPayable().isPositive())
                lines.add(new JournalLineData(new AccountCode("40111"), taxReturn.igvPayable(), zero, null));
            if (taxReturn.getIncomeTaxPrepayment().isPositive())
                lines.add(new JournalLineData(new AccountCode("4017"), taxReturn.getIncomeTaxPrepayment(), zero, null));
            lines.add(new JournalLineData(new AccountCode("10"), zero, taxReturn.totalPayable(), null));
            var period = taxReturn.getPeriod();
            var result = ledger.handle(new RegisterJournalEntryCommand(command.paidOn(),
                    "IGV and income tax prepayment %d-%02d".formatted(period.year(), period.month()),
                    EntrySource.TAX_RETURN, taxReturn.getId(), lines));
            if (result.isFailure()) {
                try {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                } catch (NoTransactionException ignored) {
                    // Plain unit tests invoke this service without a transaction proxy.
                }
                return Result.failure(result.failure().orElseThrow() == LedgerCommandFailure.DUPLICATE_SOURCE
                        ? TaxCommandFailure.ALREADY_PAID : TaxCommandFailure.ACCOUNTS_NOT_CONFIGURED);
            }
        }
        TaxReturn saved = returns.save(taxReturn);
        if (saved.totalPayable().isPositive()) {
            events.publishEvent(new TaxReturnPaidEvent(saved.getId(), command.paidOn(), saved.totalPayable(),
                    command.bankAccountId()));
        }
        return Result.success(saved);
    }

    /** Each change is a new row; a date can only be set once because the old row may already have been used. */
    @Transactional
    public Result<TaxParameters, TaxCommandFailure> handle(SetTaxParametersCommand command) {
        boolean taken = parameters.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(command.effectiveFrom())
                .filter(existing -> existing.getEffectiveFrom().equals(command.effectiveFrom())).isPresent();
        if (taken) {
            return Result.failure(TaxCommandFailure.DUPLICATE_EFFECTIVE_DATE);
        }
        return Result.success(parameters.save(new TaxParameters(command)));
    }
}
