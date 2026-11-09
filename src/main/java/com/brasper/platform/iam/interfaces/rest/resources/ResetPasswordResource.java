package com.brasper.platform.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordResource(@NotBlank String newPassword) {
}
