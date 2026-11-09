package com.brasper.platform.accounting.application.commandservices;

import com.brasper.platform.accounting.domain.model.aggregates.FixedAsset;
import com.brasper.platform.accounting.domain.model.commands.DisposeFixedAssetCommand;
import com.brasper.platform.accounting.domain.model.commands.RegisterFixedAssetCommand;
import com.brasper.platform.accounting.domain.repositories.FixedAssetRepository;
import com.brasper.platform.accounting.domain.repositories.JournalEntryRepository;
import com.brasper.platform.accounting.domain.services.PostingRules;
import com.brasper.platform.accounting.domain.repositories.TaxParametersRepository;
import com.brasper.platform.shared.application.result.Result;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import com.brasper.platform.accounting.domain.model.aggregates.JournalEntry;
import com.brasper.platform.accounting.domain.model.entities.JournalLine;
import com.brasper.platform.accounting.domain.model.commands.RunMonthlyDepreciationCommand;
import com.brasper.platform.accounting.domain.model.commands.RegisterJournalEntryCommand;
import com.brasper.platform.accounting.domain.model.commands.JournalLineData;
import com.brasper.platform.accounting.domain.model.valueobjects.AccountCode;
import com.brasper.platform.accounting.domain.model.valueobjects.EntrySource;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FixedAssetCommandService {

    private static final int MAX_USEFUL_LIFE_MONTHS = 600;

    private final FixedAssetRepository assets;
    private final TaxParametersRepository parameters;

    private final LedgerCommandService ledger;
    private final JournalEntryRepository entries;

    public FixedAssetCommandService(FixedAssetRepository assets, TaxParametersRepository parameters,
            LedgerCommandService ledger, JournalEntryRepository entries) {
        this.assets = assets;
        this.parameters = parameters;
        this.ledger = ledger;
        this.entries = entries;
    }

    @Transactional
    public Result<List<JournalEntry>, FixedAssetCommandFailure> handle(RunMonthlyDepreciationCommand command) {
        var period = command.period();
        var totals = new EnumMap<Currency, Money>(Currency.class);
        for (FixedAsset asset : assets.findAll()) {
            Money charge = asset.accumulatedDepreciationAt(period.endDate())
                    .subtract(asset.accumulatedDepreciationAt(period.previous().endDate()));
            if (charge.isPositive()) totals.merge(charge.currency(), charge, Money::add);
        }
        if (totals.isEmpty()) return Result.failure(FixedAssetCommandFailure.NOTHING_TO_DEPRECIATE);
        List<JournalEntry> entries = new ArrayList<>();
        for (Money total : totals.values()) {
            Money zero = Money.zero(total.currency());
            long sourceId = (period.year() * 100L + period.month()) * 10 + total.currency().ordinal();
            var result = ledger.handle(new RegisterJournalEntryCommand(period.endDate(),
                    "Depreciation %d-%02d".formatted(period.year(), period.month()), EntrySource.DEPRECIATION, sourceId,
                    List.of(new JournalLineData(new AccountCode("68"), total, zero, null),
                            new JournalLineData(new AccountCode("39"), zero, total, null))));
            if (result.isFailure()) {
                return Result.failure(rollBack(result.failure().orElseThrow() == LedgerCommandFailure.DUPLICATE_SOURCE
                        ? FixedAssetCommandFailure.DEPRECIATION_ALREADY_RUN : FixedAssetCommandFailure.ACCOUNTS_NOT_CONFIGURED));
            }
            entries.add(result.success().orElseThrow());
        }
        return Result.success(entries);
    }

    /** shortcut: with no tax parameters set yet the standard rates apply; remove once parameters are seeded. */
    @Transactional
    public Result<FixedAsset, FixedAssetCommandFailure> handle(RegisterFixedAssetCommand command) {
        if (!command.taxableBase().currency().isSupported()) {
            return Result.failure(FixedAssetCommandFailure.UNSUPPORTED_CURRENCY);
        }
        if (command.usefulLifeMonths() < 1 || command.usefulLifeMonths() > MAX_USEFUL_LIFE_MONTHS) {
            return Result.failure(FixedAssetCommandFailure.INVALID_USEFUL_LIFE);
        }
        TaxRates rates = parameters.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(command.acquiredOn())
                .map(found -> found.getRates()).orElseGet(TaxRates::standard);
        FixedAsset asset = assets.save(new FixedAsset(command, rates));
        var cost = asset.getAmounts().taxableBase();
        var originalCost = command.expenseId() == null ? null : costDebitOf(command.expenseId());
        var posting = ledger.handle(PostingRules.forFixedAssetAcquisition(asset.getId(), asset.getAcquiredOn(), cost,
                originalCost != null ? originalCost.getAccountCode() : new AccountCode("59"),
                originalCost != null ? originalCost.getProjectId() : null));
        if (posting.isFailure()) {
            return Result.failure(rollBack(FixedAssetCommandFailure.ACCOUNTS_NOT_CONFIGURED));
        }
        return Result.success(asset);
    }

    /** The expense that paid for the asset charged 60 or 63 when it was registered; the asset takes that cost over. */
    private JournalLine costDebitOf(Long expenseId) {
        return entries.findBySourceAndSourceId(EntrySource.EXPENSE, expenseId).stream()
                .flatMap(entry -> entry.getLines().stream())
                .filter(line -> line.getDebit().isPositive()
                        && (line.getAccountCode().equals(new AccountCode("60")) || line.getAccountCode().equals(new AccountCode("63"))))
                .findFirst().orElse(null);
    }

    private static FixedAssetCommandFailure rollBack(FixedAssetCommandFailure failure) {
        try {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } catch (NoTransactionException ignored) {
            // Plain unit tests invoke this service without a transaction proxy.
        }
        return failure;
    }

    @Transactional
    public Result<FixedAsset, FixedAssetCommandFailure> handle(DisposeFixedAssetCommand command) {
        FixedAsset asset = assets.findById(command.fixedAssetId()).orElse(null);
        if (asset == null) {
            return Result.failure(FixedAssetCommandFailure.FIXED_ASSET_NOT_FOUND);
        }
        if (asset.getDisposedOn() != null) {
            return Result.failure(FixedAssetCommandFailure.ALREADY_DISPOSED);
        }
        if (command.disposedOn().isBefore(asset.getAcquiredOn())) {
            return Result.failure(FixedAssetCommandFailure.INVALID_DISPOSAL_DATE);
        }
        asset.dispose(command.disposedOn());
        var posting = ledger.handle(PostingRules.forFixedAssetDisposal(asset.getId(), command.disposedOn(),
                asset.getAmounts().taxableBase(), asset.accumulatedDepreciationAt(command.disposedOn())));
        if (posting.isFailure()) {
            return Result.failure(rollBack(FixedAssetCommandFailure.ACCOUNTS_NOT_CONFIGURED));
        }
        return Result.success(assets.save(asset));
    }
}
