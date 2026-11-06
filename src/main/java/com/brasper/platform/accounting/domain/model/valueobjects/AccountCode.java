package com.brasper.platform.accounting.domain.model.valueobjects;

import java.util.regex.Pattern;

public record AccountCode(String value) {
    private static final Pattern VALID = Pattern.compile("[1-9][0-9]{1,9}");

    public AccountCode {
        if (value == null || !VALID.matcher(value).matches()) {
            throw new IllegalArgumentException("Account code must have 2 to 10 digits and start with 1 to 9");
        }
    }
    public int element() { return value.charAt(0) - '0'; }
}
