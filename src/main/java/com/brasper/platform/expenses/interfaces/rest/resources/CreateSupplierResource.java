package com.brasper.platform.expenses.interfaces.rest.resources;

import com.brasper.platform.expenses.domain.model.valueobjects.SupplierType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code ruc} is required for a company and optional for a natural person; blank counts as absent. */
public record CreateSupplierResource(
        @NotBlank @Size(max = 200) String businessName,
        String ruc,
        @NotNull SupplierType type,
        @Size(max = 30) String phone) {
}
