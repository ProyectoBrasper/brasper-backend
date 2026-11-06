package com.brasper.platform.accounting.application.commandservices;

import com.brasper.platform.accounting.domain.model.aggregates.FixedAsset;
import com.brasper.platform.accounting.domain.model.commands.DisposeFixedAssetCommand;
import com.brasper.platform.accounting.domain.model.commands.RegisterFixedAssetCommand;
import com.brasper.platform.accounting.domain.repositories.FixedAssetRepository;
import com.brasper.platform.accounting.domain.repositories.TaxParametersRepository;
import com.brasper.platform.shared.application.result.Result;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FixedAssetCommandService {

    private static final int MAX_USEFUL_LIFE_MONTHS = 600;

    private final FixedAssetRepository assets;
    private final TaxParametersRepository parameters;

    public FixedAssetCommandService(FixedAssetRepository assets, TaxParametersRepository parameters) {
        this.assets = assets;
        this.parameters = parameters;
    }

    /** shortcut: with no tax parameters set yet the standard rates apply; remove once parameters are seeded. */
    @Transactional
    public Result<FixedAsset, FixedAssetCommandFailure> handle(RegisterFixedAssetCommand command) {
        if (command.usefulLifeMonths() < 1 || command.usefulLifeMonths() > MAX_USEFUL_LIFE_MONTHS) {
            return Result.failure(FixedAssetCommandFailure.INVALID_USEFUL_LIFE);
        }
        TaxRates rates = parameters.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(command.acquiredOn())
                .map(found -> found.getRates()).orElseGet(TaxRates::standard);
        return Result.success(assets.save(new FixedAsset(command, rates)));
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
        return Result.success(assets.save(asset));
    }
}
