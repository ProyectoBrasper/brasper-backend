package com.brasper.platform.expenses.domain.model.commands;

import com.brasper.platform.expenses.domain.model.valueobjects.SupplierType;
import com.brasper.platform.shared.domain.model.valueobjects.Ruc;

/** {@code ruc} may be null only for a natural person. */
public record CreateSupplierCommand(String businessName, Ruc ruc, SupplierType type, String phone) {
}
