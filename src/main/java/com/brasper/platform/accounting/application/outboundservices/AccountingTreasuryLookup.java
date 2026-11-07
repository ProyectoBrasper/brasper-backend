package com.brasper.platform.accounting.application.outboundservices;

/** What Accounting needs from Treasury. The adapter lives in infrastructure and goes through the Treasury facade. */
public interface AccountingTreasuryLookup {

    boolean bankAccountExists(Long bankAccountId);
}
