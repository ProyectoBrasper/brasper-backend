package com.brasper.platform.reporting.domain.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.reporting.domain.model.readmodels.CashFlowLine;
import com.brasper.platform.reporting.domain.model.readmodels.CashFlowReport;
import com.brasper.platform.reporting.domain.model.valueobjects.CashFlowConcept;
import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CashFlowCalculatorTest {

    private static final Period MARCH = new Period(2038, 3);
    /** In the second week: the first week is past, the rest are current or future. */
    private static final LocalDate TODAY = LocalDate.of(2038, 3, 10);

    /** Real cash 1000 before March; actual: +300 collected, -100 suppliers; projected: +50 collections, -80 payables. */
    private final CashFlowSource source = new CashFlowSource() {
        @Override
        public BigDecimal cashBalanceAt(LocalDate date) {
            return new BigDecimal("1000");
        }

        @Override
        public Map<CashFlowConcept, BigDecimal> actualInflows(CashFlowWeek week) {
            return Map.of(CashFlowConcept.INVOICE_COLLECTIONS, new BigDecimal("300"));
        }

        @Override
        public Map<CashFlowConcept, BigDecimal> actualOutflows(CashFlowWeek week) {
            return Map.of(CashFlowConcept.SUPPLIERS, new BigDecimal("100"));
        }

        @Override
        public Map<CashFlowConcept, BigDecimal> projectedInflows(CashFlowWeek week) {
            return Map.of(CashFlowConcept.INVOICE_COLLECTIONS, new BigDecimal("50"));
        }

        @Override
        public Map<CashFlowConcept, BigDecimal> projectedOutflows(CashFlowWeek week) {
            return Map.of(CashFlowConcept.PENDING_PAYABLES, new BigDecimal("80"));
        }
    };

    @Test
    void chainsTheWeeksAndSwitchesToWhatIsScheduledFromTheCurrentWeek() {
        CashFlowReport report = CashFlowCalculator.buildWeekly(MARCH.weeks().subList(0, 3), source, TODAY);

        CashFlowLine past = report.lines().get(0);
        CashFlowLine current = report.lines().get(1);
        CashFlowLine future = report.lines().get(2);
        assertThat(past.projected()).isFalse();
        assertThat(past.openingBalance()).isEqualByComparingTo("1000");
        assertThat(past.closingBalance()).isEqualByComparingTo("1200");
        assertThat(current.projected()).isTrue();
        assertThat(current.openingBalance()).isEqualByComparingTo("1200");
        assertThat(current.netFlow()).isEqualByComparingTo("-30");
        assertThat(future.openingBalance()).isEqualByComparingTo("1170");
        assertThat(report.finalBalance()).isEqualByComparingTo("1140");
        assertThat(report.from()).isEqualTo(LocalDate.of(2038, 3, 1));
        assertThat(report.hasNegativeBalance()).isFalse();
    }

    @Test
    void aMonthSumsItsWeeksAndOpensWithThePreviousClosing() {
        CashFlowReport report = CashFlowCalculator.buildMonthly(List.of(MARCH, MARCH.next()), source, TODAY);

        CashFlowLine march = report.lines().get(0);
        assertThat(march.label()).isEqualTo("2038-03");
        assertThat(march.projected()).isTrue();
        assertThat(march.totalInflows()).isEqualByComparingTo("500");
        assertThat(report.lines().get(1).openingBalance()).isEqualByComparingTo(march.closingBalance());
    }

    @Test
    void aNegativeClosingBalanceIsFlagged() {
        CashFlowSource poor = new CashFlowSource() {
            @Override public BigDecimal cashBalanceAt(LocalDate date) { return new BigDecimal("10"); }
            @Override public Map<CashFlowConcept, BigDecimal> actualInflows(CashFlowWeek week) { return Map.of(); }
            @Override public Map<CashFlowConcept, BigDecimal> actualOutflows(CashFlowWeek week) { return Map.of(); }
            @Override public Map<CashFlowConcept, BigDecimal> projectedInflows(CashFlowWeek week) { return Map.of(); }
            @Override public Map<CashFlowConcept, BigDecimal> projectedOutflows(CashFlowWeek week) {
                return Map.of(CashFlowConcept.PAYROLL, new BigDecimal("500"));
            }
        };

        assertThat(CashFlowCalculator.buildWeekly(MARCH.weeks().subList(1, 2), poor, TODAY).hasNegativeBalance()).isTrue();
    }

    @Test
    void anEmptyRangeIsRejected() {
        assertThatThrownBy(() -> CashFlowCalculator.buildWeekly(List.of(), source, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CashFlowCalculator.buildMonthly(List.of(), source, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
