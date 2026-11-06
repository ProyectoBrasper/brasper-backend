package com.brasper.platform.expenses.interfaces.rest.transform;

import com.brasper.platform.expenses.domain.model.aggregates.Supplier;
import com.brasper.platform.expenses.interfaces.rest.resources.SupplierResource;

public final class SupplierResourceFromEntityAssembler {

    private SupplierResourceFromEntityAssembler() {
    }

    public static SupplierResource toResourceFromEntity(Supplier supplier) {
        return new SupplierResource(supplier.getId(), supplier.getBusinessName(),
                supplier.getRuc() == null ? null : supplier.getRuc().value(), supplier.getType().name(),
                supplier.getPhone());
    }
}
