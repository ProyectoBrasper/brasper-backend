package com.brasper.platform.reporting.domain.model.valueobjects;

/** The rows of the cash flow, as in the Excel. */
public enum CashFlowConcept {
    // inflows
    INVOICE_COLLECTIONS, LOANS, PARTNER_CONTRIBUTIONS, OTHER_INCOME,
    // outflows
    SUPPLIERS, PAYROLL, OFFICE_RENT, CLEANING, INSURANCE, TAXES, PENDING_PAYABLES, LOAN_REPAYMENTS, OTHER_EXPENSES
}
