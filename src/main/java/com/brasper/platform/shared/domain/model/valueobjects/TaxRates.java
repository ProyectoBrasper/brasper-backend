package com.brasper.platform.shared.domain.model.valueobjects;

import java.math.BigDecimal;
import java.util.Objects;

/** Peruvian tax rates in force on a date. Accounting owns them; other contexts receive a copy. */
public record TaxRates(Percentage igv, Percentage detraction, Percentage retention,
        Percentage incomeTaxPrepayment) {

    public TaxRates {
        Objects.requireNonNull(igv, "igv");
        Objects.requireNonNull(detraction, "detraction");
        Objects.requireNonNull(retention, "retention");
        Objects.requireNonNull(incomeTaxPrepayment, "incomeTaxPrepayment");
    }

    /** IGV 18%, detraction 12%, retention 3%, income tax prepayment 1.5%. */
    public static TaxRates standard() {
        return new TaxRates(Percentage.ofPercent(new BigDecimal("18")), Percentage.ofPercent(new BigDecimal("12")),
                Percentage.ofPercent(new BigDecimal("3")), Percentage.ofPercent(new BigDecimal("1.5")));
    }
}
