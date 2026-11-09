package com.brasper.platform.reporting.infrastructure.acl;

import com.brasper.platform.accounting.interfaces.acl.AccountingContextFacade;
import com.brasper.platform.reporting.application.outboundservices.StatementsSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class ReportingFacadeStatementsSource implements StatementsSource {

    private final AccountingContextFacade accounting;

    ReportingFacadeStatementsSource(AccountingContextFacade accounting) {
        this.accounting = accounting;
    }

    @Override
    public Map<String, BigDecimal> movementsBetween(LocalDate from, LocalDate to) {
        return accounting.fetchAccountMovementsBetween(from, to);
    }

    @Override
    public Map<String, BigDecimal> balancesAt(LocalDate date) {
        return accounting.fetchAccountBalancesAt(date);
    }
}
