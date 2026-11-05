package com.brasper.platform.iam.domain.model.commands;

import com.brasper.platform.iam.domain.model.valueobjects.Role;
import com.brasper.platform.iam.domain.model.valueobjects.Username;

/** The password is the raw one; the application service hashes it before building the {@code User}. */
public record SignUpCommand(Username username, String password, String fullName, Role role) {
}
