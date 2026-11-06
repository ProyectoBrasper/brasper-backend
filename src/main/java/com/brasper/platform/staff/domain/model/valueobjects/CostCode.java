package com.brasper.platform.staff.domain.model.valueobjects;

import java.util.Objects;
public record CostCode(String value) {
    public CostCode {
        value = Objects.requireNonNull(value, "value").strip();
        if (value.isBlank() || value.length() > 30) throw new IllegalArgumentException("Code must contain 1 to 30 characters");
    }
}
