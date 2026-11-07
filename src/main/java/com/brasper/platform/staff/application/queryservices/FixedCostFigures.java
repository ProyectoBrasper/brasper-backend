package com.brasper.platform.staff.application.queryservices;

import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.staff.domain.model.aggregates.FixedCost;
import com.brasper.platform.staff.domain.repositories.FixedCostRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fixed cost figures per cash-flow week for Reporting, keyed by {@code FixedCostType} name.
 * shortcut: amounts of different currencies are added as plain numbers and everything is summed in memory.
 */
@Service
@Transactional(readOnly = true)
public class FixedCostFigures {

    private final FixedCostRepository costs;

    public FixedCostFigures(FixedCostRepository costs) {
        this.costs = costs;
    }

    /** Payments made on a day of the week. */
    public Map<String, BigDecimal> paidIn(CashFlowWeek week) {
        Map<String, BigDecimal> totals = new TreeMap<>();
        for (FixedCost cost : costs.findAll()) {
            cost.getPayments().stream().filter(payment -> week.contains(payment.getPaidOn()))
                    .forEach(payment -> totals.merge(cost.getType().name(), payment.getAmount().amount(), BigDecimal::add));
        }
        return totals;
    }

    /**
     * What active costs are expected to pay in the week: the monthly amount split evenly across the weeks the cost
     * is expected in, while the month is not fully paid yet.
     * shortcut: payments already made are not discounted week by week, only against the whole month.
     */
    public Map<String, BigDecimal> scheduledIn(CashFlowWeek week) {
        Map<String, BigDecimal> totals = new TreeMap<>();
        for (FixedCost cost : costs.findAllByActive(true)) {
            var expected = cost.getExpectedWeeks();
            if (expected.contains(week.number()) && cost.pendingIn(week.period()).isPositive()) {
                BigDecimal share = cost.getMonthlyAmount().amount()
                        .divide(BigDecimal.valueOf(expected.size()), 2, RoundingMode.HALF_UP);
                totals.merge(cost.getType().name(), share, BigDecimal::add);
            }
        }
        return totals;
    }
}
