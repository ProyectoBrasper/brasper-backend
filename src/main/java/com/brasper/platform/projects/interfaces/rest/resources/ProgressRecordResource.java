package com.brasper.platform.projects.interfaces.rest.resources;

import java.math.BigDecimal;

public record ProgressRecordResource(Long id, int year, int month, BigDecimal percentage) {
}
