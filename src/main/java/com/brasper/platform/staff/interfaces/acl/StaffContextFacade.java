package com.brasper.platform.staff.interfaces.acl;

import java.math.BigDecimal;
import java.util.Map;

/** The only way other contexts read Staff's fixed costs. Both maps are keyed by {@code FixedCostType} name. */
public interface StaffContextFacade {

    /** Fixed cost payments made during the cash-flow week. */
    Map<String, BigDecimal> fetchFixedCostsPaidByWeek(int year, int month, int week);

    /** Fixed costs still expected in the cash-flow week (active costs whose month is not fully paid). */
    Map<String, BigDecimal> fetchFixedCostsScheduledByWeek(int year, int month, int week);
}
