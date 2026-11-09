package com.brasper.platform.billing.domain.model.valueobjects;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** SUNAT invoice number: a 4-character series (F001) and a correlative of up to 8 digits. Printed as F001-00000123. */
public record InvoiceNumber(String series, int correlative) {

    private static final Pattern SERIES = Pattern.compile("[A-Z0-9]{4}");
    private static final int MAX_CORRELATIVE = 99_999_999;

    public InvoiceNumber {
        Objects.requireNonNull(series, "series");
        series = series.strip().toUpperCase(Locale.ROOT);
        if (!SERIES.matcher(series).matches()) {
            throw new IllegalArgumentException("Series must be 4 letters or digits, like F001");
        }
        if (correlative < 1 || correlative > MAX_CORRELATIVE) {
            throw new IllegalArgumentException("Correlative must be between 1 and " + MAX_CORRELATIVE);
        }
    }

    @Override
    public String toString() {
        return "%s-%08d".formatted(series, correlative);
    }
}
