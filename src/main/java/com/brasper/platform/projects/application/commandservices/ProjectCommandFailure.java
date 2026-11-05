package com.brasper.platform.projects.application.commandservices;

import com.brasper.platform.shared.application.result.CommandFailure;

public enum ProjectCommandFailure implements CommandFailure {
    DUPLICATE_CODE("projects.error.duplicate-code"),
    CLIENT_NOT_FOUND("projects.error.client-not-found"),
    PROJECT_NOT_FOUND("projects.error.project-not-found"),
    INVALID_STATUS_TRANSITION("projects.error.invalid-status-transition");

    private final String messageKey;

    ProjectCommandFailure(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }
}
