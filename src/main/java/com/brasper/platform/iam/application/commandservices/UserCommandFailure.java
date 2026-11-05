package com.brasper.platform.iam.application.commandservices;

import com.brasper.platform.shared.application.result.CommandFailure;

public enum UserCommandFailure implements CommandFailure {
    INVALID_CREDENTIALS("iam.error.invalid-credentials"),
    USER_INACTIVE("iam.error.user-inactive"),
    DUPLICATE_USERNAME("iam.error.duplicate-username"),
    WEAK_PASSWORD("iam.error.weak-password");

    private final String messageKey;

    UserCommandFailure(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }
}
