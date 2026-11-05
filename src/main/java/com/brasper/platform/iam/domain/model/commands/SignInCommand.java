package com.brasper.platform.iam.domain.model.commands;

import com.brasper.platform.iam.domain.model.valueobjects.Username;

public record SignInCommand(Username username, String password) {
}
