package com.brasper.platform.projects.interfaces.acl;

import java.math.BigDecimal;

/**
 * What other contexts may know about a project. {@code budget} and {@code estimatedCost} are in the project's
 * {@code currency}; {@code participation}, {@code salesCommissionRate} and {@code accumulatedProgress} are fractions.
 */
public record ProjectSnapshot(Long id, String code, String name, String clientName, String status,
        BigDecimal budget, String currency, BigDecimal participation, BigDecimal salesCommissionRate,
        BigDecimal accumulatedProgress, BigDecimal estimatedCost) {
}
