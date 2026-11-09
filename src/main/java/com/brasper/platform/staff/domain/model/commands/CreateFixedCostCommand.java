package com.brasper.platform.staff.domain.model.commands;

import com.brasper.platform.staff.domain.model.valueobjects.*;
import com.brasper.platform.shared.domain.model.valueobjects.Money;

public record CreateFixedCostCommand(CostCode code, String concept, FixedCostType type, Money monthlyAmount,
        Long employeeId, PaymentSchedule schedule) {}
