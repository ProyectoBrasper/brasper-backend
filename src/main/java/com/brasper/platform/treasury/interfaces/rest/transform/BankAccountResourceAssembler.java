package com.brasper.platform.treasury.interfaces.rest.transform;

import com.brasper.platform.treasury.application.queryservices.BankAccountSummary;
import com.brasper.platform.treasury.domain.model.entities.CashMovement;
import com.brasper.platform.treasury.interfaces.rest.resources.BankAccountResource;
import com.brasper.platform.treasury.interfaces.rest.resources.CashMovementResource;

public final class BankAccountResourceAssembler {

    private BankAccountResourceAssembler() {
    }

    public static BankAccountResource toResource(BankAccountSummary summary) {
        var account = summary.account();
        return new BankAccountResource(account.getId(), account.getBank().name(), account.getAccountNumber().value(),
                account.getType().name(), account.getCurrency().name(), account.getOpeningBalance().amount(),
                summary.currentBalance().amount());
    }

    public static CashMovementResource toResource(CashMovement movement) {
        return new CashMovementResource(movement.getId(), movement.getOccurredOn(), movement.getDirection().name(),
                movement.getAmount().amount(), movement.getConcept(), movement.getSource().name(),
                movement.getSourceId());
    }
}
