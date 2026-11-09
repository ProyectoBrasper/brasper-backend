package com.brasper.platform.billing.application.outboundservices;

import java.util.Optional;

/** What Billing needs from Projects. The adapter lives in infrastructure and goes through the Projects facade. */
public interface ProjectLookup {

    Optional<BillableProject> find(Long projectId);

    /** {@code status} is the project's status name, e.g. {@code IN_PROGRESS}. */
    record BillableProject(Long projectId, Long clientId, String status) {

        /** Only a project that has started can be invoiced: not one that is just quoted or was cancelled. */
        public boolean canBeInvoiced() {
            return status.equals("IN_PROGRESS") || status.equals("COMPLETED");
        }
    }
}
