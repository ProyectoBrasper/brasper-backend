package com.brasper.platform.billing.domain.model.valueobjects;

public enum InvoicePaymentType {
    /** The client deposits the net amount in the company's checking account. */
    NET_DEPOSIT,
    /** The client deposits the detraction in the company's Banco de la Nación account. */
    DETRACTION_DEPOSIT,
    /** The client keeps the retention and delivers a certificate; no money moves to the company. */
    RETENTION_CERTIFICATE
}
