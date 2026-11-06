package com.brasper.platform.expenses.domain.model.commands;

/** The RUC and the type identify the supplier and cannot be changed. */
public record UpdateSupplierCommand(Long supplierId, String businessName, String phone) {
}
