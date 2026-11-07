package com.brasper.platform.reporting.infrastructure.acl;

import com.brasper.platform.billing.interfaces.acl.BillingContextFacade;
import com.brasper.platform.reporting.application.outboundservices.DashboardSource;
import com.brasper.platform.treasury.interfaces.acl.TreasuryContextFacade;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
class ReportingFacadeDashboardSource implements DashboardSource {

    private final TreasuryContextFacade treasury;
    private final BillingContextFacade billing;

    ReportingFacadeDashboardSource(TreasuryContextFacade treasury, BillingContextFacade billing) {
        this.treasury = treasury;
        this.billing = billing;
    }

    @Override
    public BigDecimal cashBalanceAt(LocalDate date) {
        return treasury.fetchCashBalanceAt(date);
    }

    @Override
    public BigDecimal receivablesOutstandingAt(LocalDate date) {
        return billing.sumOutstandingReceivablesAt(date);
    }

    @Override
    public BigDecimal payablesOutstanding() {
        return treasury.sumOutstandingPayables();
    }
}
