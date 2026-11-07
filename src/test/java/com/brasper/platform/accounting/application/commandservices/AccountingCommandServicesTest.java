package com.brasper.platform.accounting.application.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.brasper.platform.accounting.domain.model.aggregates.FixedAsset;
import com.brasper.platform.accounting.domain.model.aggregates.TaxParameters;
import com.brasper.platform.accounting.domain.model.commands.DisposeFixedAssetCommand;
import com.brasper.platform.accounting.domain.model.commands.RegisterFixedAssetCommand;
import com.brasper.platform.accounting.domain.model.commands.SetTaxParametersCommand;
import com.brasper.platform.accounting.domain.repositories.FixedAssetRepository;
import com.brasper.platform.accounting.domain.repositories.TaxParametersRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AccountingCommandServicesTest {

    private static final LocalDate DAY = LocalDate.of(2026, 3, 10);

    private final TaxParametersRepository parameters = mock(TaxParametersRepository.class);
    private final FixedAssetRepository assets = mock(FixedAssetRepository.class);
    private final TaxCommandService tax = new TaxCommandService(parameters);
    private final FixedAssetCommandService fixedAssets = new FixedAssetCommandService(assets, parameters, mock(LedgerCommandService.class));

    private static RegisterFixedAssetCommand register(int months) {
        return new RegisterFixedAssetCommand("Laptop", DAY, new Money(new BigDecimal("1200"), Currency.PEN), months,
                null);
    }

    @Test
    void setsTaxParametersOnceForEachEffectiveDate() {
        when(parameters.save(any(TaxParameters.class))).thenAnswer(call -> call.getArgument(0));
        var command = new SetTaxParametersCommand(TaxRates.standard(), DAY);

        assertThat(tax.handle(command).isSuccess()).isTrue();
        when(parameters.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(DAY))
                .thenReturn(Optional.of(new TaxParameters(command)));

        assertThat(tax.handle(command).failure()).contains(TaxCommandFailure.DUPLICATE_EFFECTIVE_DATE);
        assertThat(tax.handle(new SetTaxParametersCommand(TaxRates.standard(), DAY.plusDays(1))).isSuccess()).isTrue();
    }

    @Test
    void registersAnAssetWithTheRatesInForceOrTheStandardOnes() {
        when(assets.save(any(FixedAsset.class))).thenAnswer(call -> call.getArgument(0));

        FixedAsset asset = fixedAssets.handle(register(36)).success().orElseThrow();

        assertThat(asset.getAmounts().igv().amount()).isEqualByComparingTo("216");
        assertThat(fixedAssets.handle(register(0)).failure()).contains(FixedAssetCommandFailure.INVALID_USEFUL_LIFE);
        assertThat(fixedAssets.handle(register(601)).failure()).contains(FixedAssetCommandFailure.INVALID_USEFUL_LIFE);
    }

    @Test
    void disposesOnceAndRejectsUnknownAssetsAndEarlyDates() {
        FixedAsset asset = new FixedAsset(register(36), TaxRates.standard());
        when(assets.findById(1L)).thenReturn(Optional.of(asset));
        when(assets.findById(9L)).thenReturn(Optional.empty());
        when(assets.save(any(FixedAsset.class))).thenAnswer(call -> call.getArgument(0));

        assertThat(fixedAssets.handle(new DisposeFixedAssetCommand(9L, DAY)).failure())
                .contains(FixedAssetCommandFailure.FIXED_ASSET_NOT_FOUND);
        assertThat(fixedAssets.handle(new DisposeFixedAssetCommand(1L, DAY.minusDays(1))).failure())
                .contains(FixedAssetCommandFailure.INVALID_DISPOSAL_DATE);
        assertThat(fixedAssets.handle(new DisposeFixedAssetCommand(1L, DAY.plusMonths(6))).isSuccess()).isTrue();
        assertThat(fixedAssets.handle(new DisposeFixedAssetCommand(1L, DAY.plusMonths(7))).failure())
                .contains(FixedAssetCommandFailure.ALREADY_DISPOSED);
    }
}
