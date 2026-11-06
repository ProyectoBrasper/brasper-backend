package com.brasper.platform.staff.domain.model.valueobjects;

import java.util.regex.Pattern;

public record Dni(String value) {
    private static final Pattern VALID = Pattern.compile("[0-9]{8}");

    public Dni {
        if (value == null || !VALID.matcher(value).matches()) {
            throw new IllegalArgumentException("DNI must have exactly 8 digits");
        }
    }
}
