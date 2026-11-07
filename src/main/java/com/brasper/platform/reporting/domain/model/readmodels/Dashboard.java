package com.brasper.platform.reporting.domain.model.readmodels;

import java.math.BigDecimal;
import java.util.List;

/** The manager's summary of a month, in PEN. {@code alerts} are codes such as {@code NEGATIVE_PROJECTED_BALANCE}. */
public record Dashboard(BigDecimal cashBalance, BigDecimal receivablesOutstanding, BigDecimal payablesOutstanding,
        BigDecimal netProfitYtd, BigDecimal projectedClosingBalance, int projectsInProgress, List<String> alerts) {

    public Dashboard {
        alerts = List.copyOf(alerts);
    }
}
