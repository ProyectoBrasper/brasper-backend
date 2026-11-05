package com.brasper.platform.projects.application.commandservices;

import com.brasper.platform.shared.application.result.CommandFailure;

public enum ClientCommandFailure implements CommandFailure {
    DUPLICATE_RUC("projects.error.duplicate-ruc"),
    CLIENT_NOT_FOUND("projects.error.client-not-found");

    private final String messageKey;

    ClientCommandFailure(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }
}
