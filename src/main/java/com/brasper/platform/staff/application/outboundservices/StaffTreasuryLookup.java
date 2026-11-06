package com.brasper.platform.staff.application.outboundservices;

/** What Staff needs from Treasury. The adapter lives in infrastructure and goes through the Treasury facade. */
public interface StaffTreasuryLookup {

    boolean bankAccountExists(Long bankAccountId);
}
