package com.brasper.platform.shared.domain.model.valueobjects;

/** Whether an obligation (an expense, a payable) has been settled. */
public enum PaymentStatus {
    PENDING,
    PARTIALLY_PAID,
    PAID
}
