package com.brasper.platform.shared.domain.model.valueobjects;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Peruvian taxpayer number: 11 digits.
 * shortcut: no check-digit (modulus 11) validation, add it if typos in RUCs become a problem.
 */
public record Ruc(String value) {

    private static final Pattern VALID = Pattern.compile("\\d{11}");

    public Ruc {
        Objects.requireNonNull(value, "value");
        value = value.strip();
        if (!VALID.matcher(value).matches()) {
            throw new IllegalArgumentException("RUC must have exactly 11 digits");
        }
    }
}
