package com.brasper.platform.accounting.interfaces.rest.transform;
import com.brasper.platform.accounting.domain.model.aggregates.TaxParameters;
import com.brasper.platform.accounting.interfaces.rest.resources.TaxParametersResource;

public final class TaxParametersResourceAssembler {
    private TaxParametersResourceAssembler() { }
    public static TaxParametersResource toResource(TaxParameters parameters) {
        var rates = parameters.getRates();
        return new TaxParametersResource(parameters.getId(), parameters.getEffectiveFrom(), rates.igv().value(),
                rates.detraction().value(), rates.retention().value(), rates.incomeTaxPrepayment().value());
    }
}
