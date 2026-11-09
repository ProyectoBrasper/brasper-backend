package com.brasper.platform.expenses.application.outboundservices;

import java.util.Optional;

/** What Expenses needs from Projects. The adapter lives in infrastructure and goes through the Projects facade. */
public interface ExpenseProjectLookup {

    /** The project's status name, e.g. {@code IN_PROGRESS}; empty when the project does not exist. */
    Optional<String> findStatus(Long projectId);
}
