package com.brasper.platform.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record SignInResource(@NotBlank String username, @NotBlank String password) {
}
