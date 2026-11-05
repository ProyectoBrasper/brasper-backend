package com.brasper.platform.iam.domain.model.commands;

import com.brasper.platform.iam.domain.model.valueobjects.Role;

public record ChangeUserRoleCommand(Long userId, Role role) {
}
