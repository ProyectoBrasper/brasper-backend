package com.brasper.platform.reporting.application.outboundservices;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** What the project profitability report needs from Projects, Billing, Expenses and Accounting. */
public interface ProfitabilitySource {

    Optional<ProjectData> project(Long projectId);

    /** Every project, whatever its status. */
    List<ProjectData> allProjects();

    /** Progress earned in the month (a fraction) per project id; projects without it are left out. */
    Map<Long, BigDecimal> progressByProject(int year, int month);

    BigDecimal invoiced(Long projectId);

    BigDecimal collected(Long projectId);

    BigDecimal directCost(Long projectId);

    Map<String, BigDecimal> directCostByCategory(Long projectId);

    /** IGV rate in force today, as a fraction. */
    BigDecimal igvRate();

    /** {@code participation}, {@code salesCommissionRate} and {@code accumulatedProgress} are fractions. */
    record ProjectData(Long id, String code, String name, String clientName, String status, String currency,
            BigDecimal budget, BigDecimal estimatedCost, BigDecimal participation, BigDecimal salesCommissionRate,
            BigDecimal accumulatedProgress) { }
}
