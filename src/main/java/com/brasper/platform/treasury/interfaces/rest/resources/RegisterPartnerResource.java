package com.brasper.platform.treasury.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterPartnerResource(@NotBlank @Size(max = 200) String fullName, @Size(max = 100) String role) {
}
