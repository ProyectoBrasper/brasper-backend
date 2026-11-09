package com.brasper.platform.expenses.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSupplierResource(
        @NotBlank @Size(max = 200) String businessName,
        @Size(max = 30) String phone) {
}
