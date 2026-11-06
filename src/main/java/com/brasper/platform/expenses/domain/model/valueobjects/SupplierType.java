package com.brasper.platform.expenses.domain.model.valueobjects;

public enum SupplierType {
    /** A business: it must have a RUC. */
    COMPANY,
    /** An individual, e.g. a day laborer or a fee-receipt issuer; the RUC is optional. */
    NATURAL_PERSON
}
