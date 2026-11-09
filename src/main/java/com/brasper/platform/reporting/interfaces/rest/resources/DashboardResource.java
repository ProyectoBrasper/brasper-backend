package com.brasper.platform.reporting.interfaces.rest.resources;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResource(BigDecimal cashBalance, BigDecimal receivablesOutstanding,
        BigDecimal payablesOutstanding, BigDecimal netProfitYtd, BigDecimal projectedClosingBalance,
        int projectsInProgress, List<String> alerts) { }
