package com.brasper.platform.staff.domain.model.valueobjects;

import java.util.HashSet;
import java.util.List;
public record PaymentSchedule(List<Integer> expectedWeeks) {
    public PaymentSchedule {
        expectedWeeks = List.copyOf(expectedWeeks);
        if (expectedWeeks.stream().anyMatch(w -> w < 1 || w > 5)
                || new HashSet<>(expectedWeeks).size() != expectedWeeks.size()) {
            throw new IllegalArgumentException("Expected weeks must be unique and between 1 and 5");
        }
    }
}
