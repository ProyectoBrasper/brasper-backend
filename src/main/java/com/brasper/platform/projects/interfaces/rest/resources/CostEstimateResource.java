package com.brasper.platform.projects.interfaces.rest.resources;

import java.math.BigDecimal;

/** The amount is in the project's currency. */
public record CostEstimateResource(Long id, String category, BigDecimal estimatedAmount) {
}
