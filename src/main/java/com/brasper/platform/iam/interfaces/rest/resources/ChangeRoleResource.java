package com.brasper.platform.iam.interfaces.rest.resources;

import com.brasper.platform.iam.domain.model.valueobjects.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleResource(@NotNull Role role) {
}
