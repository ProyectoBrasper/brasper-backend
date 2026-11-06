package com.brasper.platform.expenses.application.outboundservices;

/** What Expenses needs from Treasury. The adapter lives in infrastructure and goes through the Treasury facade. */
public interface ExpenseTreasuryLookup {

    boolean bankAccountExists(Long bankAccountId);

    boolean partnerExists(Long partnerId);
}
