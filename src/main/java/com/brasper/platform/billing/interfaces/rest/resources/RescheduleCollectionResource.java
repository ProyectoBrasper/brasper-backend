package com.brasper.platform.billing.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RescheduleCollectionResource(@NotNull LocalDate expectedOn) {
}
