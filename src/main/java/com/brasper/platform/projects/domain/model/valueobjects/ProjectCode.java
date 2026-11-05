package com.brasper.platform.projects.domain.model.valueobjects;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Format {@code NNNBR-MMM-AAAA}, e.g. {@code 862BR-AGO-2026}: sequence, Spanish month, year.
 * Rejects the typos seen in the Excel ("8789R-OCT-2026", "853BRJUN-2026").
 */
public record ProjectCode(String value) {

    private static final String MONTHS = "ENEFEBMARABRMAYJUNJULAGOSEPOCTNOVDIC";
    private static final Pattern VALID = Pattern.compile(
            "\\d{3}BR-(ENE|FEB|MAR|ABR|MAY|JUN|JUL|AGO|SEP|OCT|NOV|DIC)-\\d{4}");

    public ProjectCode {
        Objects.requireNonNull(value, "value");
        value = value.strip().toUpperCase(Locale.ROOT);
        if (!VALID.matcher(value).matches()) {
            throw new IllegalArgumentException("Project code must look like 862BR-AGO-2026");
        }
    }

    public int sequence() {
        return Integer.parseInt(value.substring(0, 3));
    }

    /** 1 (ENE) to 12 (DIC). */
    public int month() {
        return MONTHS.indexOf(value.substring(6, 9)) / 3 + 1;
    }

    public int year() {
        return Integer.parseInt(value.substring(10));
    }
}
