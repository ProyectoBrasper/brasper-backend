package com.brasper.platform.staff.domain.model.commands;

import com.brasper.platform.staff.domain.model.valueobjects.PaymentSchedule;
import com.brasper.platform.shared.domain.model.valueobjects.Money;

public record UpdateFixedCostCommand(Long fixedCostId, String concept, Money monthlyAmount, PaymentSchedule schedule) {}
