package com.brasper.platform.iam.domain.model.valueobjects;

/** Spring Security style names so they can be used directly with hasRole(...). */
public enum Role {
    /** Manages users and tax parameters. */
    ROLE_ADMIN,
    /** Sees every report. */
    ROLE_MANAGEMENT,
    /** Invoicing, expenses, treasury and accounting. */
    ROLE_ACCOUNTING,
    /** Projects, progress and project expenses. */
    ROLE_ENGINEER
}
