package com.brasper.platform.expenses.domain.model.valueobjects;

import java.util.Objects;

/** The document backing an expense. Everything but {@code NO_VOUCHER} carries a number. */
public record Voucher(VoucherType type, String number) {

    static final int MAX_NUMBER_LENGTH = 30;

    public Voucher {
        Objects.requireNonNull(type, "type");
        number = number == null || number.isBlank() ? null : number.strip();
        if (type == VoucherType.NO_VOUCHER && number != null) {
            throw new IllegalArgumentException("An expense without voucher has no voucher number");
        }
        if (type != VoucherType.NO_VOUCHER && number == null) {
            throw new IllegalArgumentException("A voucher number is required");
        }
        if (number != null && number.length() > MAX_NUMBER_LENGTH) {
            throw new IllegalArgumentException("Voucher number is too long");
        }
    }

    /** Only an invoice gives IGV credit. */
    public boolean allowsTaxCredit() {
        return type == VoucherType.INVOICE;
    }
}
