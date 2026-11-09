package com.brasper.platform.iam.application.commandservices;

import com.brasper.platform.shared.application.result.CommandFailure;

public enum UserCommandFailure implements CommandFailure {
    INVALID_CREDENTIALS("iam.error.invalid-credentials"),
    USER_INACTIVE("iam.error.user-inactive"),
    TOO_MANY_ATTEMPTS("iam.error.too-many-attempts"),
    DUPLICATE_USERNAME("iam.error.duplicate-username"),
    WEAK_PASSWORD("iam.error.weak-password"),
    USER_NOT_FOUND("iam.error.user-not-found"),
    CANNOT_MODIFY_LAST_ADMIN("iam.error.cannot-modify-last-admin");

    private final String messageKey;

    UserCommandFailure(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }
}
