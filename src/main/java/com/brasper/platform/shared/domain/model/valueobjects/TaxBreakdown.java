package com.brasper.platform.shared.domain.model.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** A taxable base, its IGV and the total. All three are in the same currency. */
public record TaxBreakdown(Money taxableBase, Money igv, Money total) {

    public TaxBreakdown {
        Objects.requireNonNull(taxableBase, "taxableBase");
        Objects.requireNonNull(igv, "igv");
        Objects.requireNonNull(total, "total");
        if (!taxableBase.add(igv).equals(total)) {
            throw new IllegalArgumentException("Total must be taxable base plus IGV");
        }
    }

    public static TaxBreakdown fromTaxableBase(Money taxableBase, Percentage igvRate) {
        Money igv = igvRate.of(taxableBase);
        return new TaxBreakdown(taxableBase, igv, taxableBase.add(igv));
    }

    /** The base is the total divided by (1 + rate); the IGV is what is left, so the three always add up. */
    public static TaxBreakdown fromTotal(Money total, Percentage igvRate) {
        BigDecimal base = total.amount().divide(BigDecimal.ONE.add(igvRate.value()), 2, RoundingMode.HALF_UP);
        Money taxableBase = new Money(base, total.currency());
        return new TaxBreakdown(taxableBase, total.subtract(taxableBase), total);
    }

    /** Expenses without an invoice: no IGV, the total is the base. */
    public static TaxBreakdown withoutIgv(Money amount) {
        return new TaxBreakdown(amount, Money.zero(amount.currency()), amount);
    }
}
