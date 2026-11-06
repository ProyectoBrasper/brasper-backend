package com.brasper.platform.treasury.domain.model.valueobjects;

import java.util.Objects;

public record AccountNumber(String value) {

    public AccountNumber {
        value = Objects.requireNonNull(value, "value").strip();
        if (value.isEmpty() || value.length() > 50) {
            throw new IllegalArgumentException("Account number must contain 1 to 50 characters");
        }
    }
}
