package com.brasper.platform.billing.application.commandservices;

import com.brasper.platform.shared.application.result.CommandFailure;

public enum ScheduledCollectionCommandFailure implements CommandFailure {
    PROJECT_NOT_FOUND("billing.error.project-not-found"),
    NOT_FOUND("billing.error.schedule-not-found"),
    /** Realized or cancelled: it can no longer be moved or cancelled. */
    ALREADY_CLOSED("billing.error.schedule-closed");

    private final String messageKey;

    ScheduledCollectionCommandFailure(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }
}
