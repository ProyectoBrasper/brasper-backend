package com.brasper.platform.reporting.domain.services;

import com.brasper.platform.reporting.domain.model.valueobjects.CashFlowConcept;
import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** Where the calculator gets its figures. The adapter composes them from the other contexts. */
public interface CashFlowSource {

    /** Real cash in the bank accounts at the end of the date. */
    BigDecimal cashBalanceAt(LocalDate date);

    Map<CashFlowConcept, BigDecimal> actualInflows(CashFlowWeek week);

    Map<CashFlowConcept, BigDecimal> actualOutflows(CashFlowWeek week);

    Map<CashFlowConcept, BigDecimal> projectedInflows(CashFlowWeek week);

    Map<CashFlowConcept, BigDecimal> projectedOutflows(CashFlowWeek week);
}
