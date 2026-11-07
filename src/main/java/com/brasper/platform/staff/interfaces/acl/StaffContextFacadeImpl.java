package com.brasper.platform.staff.interfaces.acl;

import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.staff.application.queryservices.FixedCostFigures;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Lives in {@code interfaces} because the layer rules forbid application from depending on the facade contract. */
@Service
public class StaffContextFacadeImpl implements StaffContextFacade {

    private final FixedCostFigures figures;

    public StaffContextFacadeImpl(FixedCostFigures figures) {
        this.figures = figures;
    }

    @Override
    public Map<String, BigDecimal> fetchFixedCostsPaidByWeek(int year, int month, int week) {
        return figures.paidIn(new CashFlowWeek(year, month, week));
    }

    @Override
    public Map<String, BigDecimal> fetchFixedCostsScheduledByWeek(int year, int month, int week) {
        return figures.scheduledIn(new CashFlowWeek(year, month, week));
    }
}
