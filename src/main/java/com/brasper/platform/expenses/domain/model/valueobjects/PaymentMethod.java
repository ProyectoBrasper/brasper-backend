package com.brasper.platform.expenses.domain.model.valueobjects;

public enum PaymentMethod {
    /** Paid from a bank account of the company: needs the account id. */
    BANK_ACCOUNT,
    /** Paid from the petty cash account: needs the account id. */
    PETTY_CASH,
    /** A partner paid with their own money (the Excel's "Aportes Ingeniero"): needs the partner id. */
    PARTNER_CONTRIBUTION
}
