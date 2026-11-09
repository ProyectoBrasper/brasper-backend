package com.brasper.platform.reporting.interfaces.rest.resources;

import java.math.BigDecimal;
import java.util.Map;

public record ProjectProfitabilityResource(Long projectId, String code, String name, String clientName, String status,
        String currency, BigDecimal budget, BigDecimal estimatedCost, BigDecimal actualCost,
        Map<String, BigDecimal> actualCostByCategory, BigDecimal costDeviation, BigDecimal salesCommission,
        BigDecimal grossProfit, BigDecimal margin, BigDecimal accumulatedProgress,
        BigDecimal recognizedProfitInPeriod, BigDecimal invoiced, BigDecimal collected) { }
