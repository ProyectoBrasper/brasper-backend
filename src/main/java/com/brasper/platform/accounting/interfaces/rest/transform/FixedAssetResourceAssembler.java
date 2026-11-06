package com.brasper.platform.accounting.interfaces.rest.transform;
import com.brasper.platform.accounting.domain.model.aggregates.FixedAsset;
import com.brasper.platform.accounting.interfaces.rest.resources.FixedAssetResource;
import java.time.LocalDate;

public final class FixedAssetResourceAssembler {
    private FixedAssetResourceAssembler() { }
    public static FixedAssetResource toResource(FixedAsset asset) {
        var amounts = asset.getAmounts();
        var today = LocalDate.now();
        return new FixedAssetResource(asset.getId(), asset.getDescription(), asset.getAcquiredOn(),
                amounts.taxableBase().amount(), amounts.igv().amount(), amounts.total().amount(),
                amounts.taxableBase().currency().name(), asset.getUsefulLifeMonths(), asset.getExpenseId(),
                asset.getDisposedOn(), asset.monthlyDepreciation().amount(),
                asset.accumulatedDepreciationAt(today).amount(), asset.bookValueAt(today).amount());
    }
}
