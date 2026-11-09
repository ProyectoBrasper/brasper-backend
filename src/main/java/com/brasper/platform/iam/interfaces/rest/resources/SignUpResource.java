package com.brasper.platform.iam.interfaces.rest.resources;

import com.brasper.platform.iam.domain.model.valueobjects.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignUpResource(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank @Size(max = 150) String fullName,
        @NotNull Role role) {
}
