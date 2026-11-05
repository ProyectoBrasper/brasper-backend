package com.brasper.platform.projects.domain.model.valueobjects;

public enum ProjectStatus {
    QUOTED,
    IN_PROGRESS,
    /** Reached when progress hits 100%. */
    COMPLETED,
    CANCELLED;

    /** Open projects can still be edited and cancelled. */
    public boolean isOpen() {
        return this == QUOTED || this == IN_PROGRESS;
    }
}
