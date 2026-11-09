package com.brasper.platform.accounting.interfaces.rest.transform;
import com.brasper.platform.accounting.domain.model.aggregates.TaxReturn;
import com.brasper.platform.accounting.interfaces.rest.resources.TaxReturnResource;

public final class TaxReturnResourceAssembler {
    private TaxReturnResourceAssembler() { }
    public static TaxReturnResource toResource(TaxReturn taxReturn) {
        return new TaxReturnResource(taxReturn.getId(), taxReturn.getPeriod().year(), taxReturn.getPeriod().month(),
                LedgerResourceAssembler.toResource(taxReturn.getOutputIgv()), LedgerResourceAssembler.toResource(taxReturn.getInputIgv()),
                LedgerResourceAssembler.toResource(taxReturn.getCarriedCredit()), LedgerResourceAssembler.toResource(taxReturn.igvPayable()),
                LedgerResourceAssembler.toResource(taxReturn.getIncomeTaxPrepayment()), LedgerResourceAssembler.toResource(taxReturn.totalPayable()),
                taxReturn.getStatus().name(), taxReturn.getPaidOn(), taxReturn.getBankAccountId());
    }
}
