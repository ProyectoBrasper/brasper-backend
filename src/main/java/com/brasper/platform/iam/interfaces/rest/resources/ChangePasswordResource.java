package com.brasper.platform.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordResource(@NotBlank String currentPassword, @NotBlank String newPassword) {
}
