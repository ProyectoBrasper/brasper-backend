package com.brasper.platform.shared.domain.model.valueobjects;

public enum Currency {
    PEN,
    USD;

    /** shortcut: reports, IGV and balances sum PEN only; accept USD once an exchange rate is stored with the entry. */
    public boolean isSupported() {
        return this == PEN;
    }
}
