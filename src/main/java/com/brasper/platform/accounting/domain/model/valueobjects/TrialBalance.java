package com.brasper.platform.accounting.domain.model.valueobjects;

import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.util.List;

public record TrialBalance(Period period, List<TrialBalanceLine> lines) {
    public TrialBalance {
        lines = List.copyOf(lines);
    }
}
