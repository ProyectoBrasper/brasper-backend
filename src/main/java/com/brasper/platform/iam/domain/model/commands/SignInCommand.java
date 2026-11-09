package com.brasper.platform.iam.domain.model.commands;

import com.brasper.platform.iam.domain.model.valueobjects.Username;

/** {@code clientIp} is where the request came from; failed attempts are counted per username and address. */
public record SignInCommand(Username username, String password, String clientIp) {
}
