package com.brasper.platform.billing.domain.model.valueobjects;

public enum ScheduleStatus {
    SCHEDULED,
    /** Moved to another week at least once; still expected. */
    RESCHEDULED,
    /** The real invoice was issued. */
    REALIZED,
    CANCELLED;

    /** A collection still expected can be moved or cancelled; a realized or cancelled one is closed. */
    public boolean isOpen() {
        return this == SCHEDULED || this == RESCHEDULED;
    }
}
