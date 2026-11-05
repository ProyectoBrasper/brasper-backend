package com.brasper.platform.projects.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectResource(Long id, String code, String name, String description, Long clientId,
        BigDecimal budget, String currency, LocalDate quotedOn, String status, BigDecimal participation,
        BigDecimal salesCommissionRate, BigDecimal accumulatedProgress) {
}
