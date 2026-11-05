package com.brasper.platform.iam.domain.model.valueobjects;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Login name: 3-50 chars of letters, digits, dot, underscore or dash. Stored lowercase so it is unique ignoring case. */
public record Username(String value) {

    private static final Pattern VALID = Pattern.compile("[a-z0-9._-]{3,50}");

    public Username {
        Objects.requireNonNull(value, "value");
        value = value.strip().toLowerCase(Locale.ROOT);
        if (!VALID.matcher(value).matches()) {
            throw new IllegalArgumentException("Username must be 3-50 characters: letters, digits, '.', '_' or '-'");
        }
    }
}
