package com.brasper.platform.reporting.application.queryservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.brasper.platform.reporting.domain.model.readmodels.CashFlowReport;
import com.brasper.platform.reporting.domain.services.CashFlowSource;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CashFlowQueryServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2038, 3, 10);

    private final CashFlowSource source = mock(CashFlowSource.class);
    private final CashFlowQueryService service = new CashFlowQueryService(source);

    CashFlowQueryServiceTest() {
        when(source.cashBalanceAt(any())).thenReturn(new BigDecimal("100"));
        when(source.actualInflows(any())).thenReturn(Map.of());
        when(source.actualOutflows(any())).thenReturn(Map.of());
        when(source.projectedInflows(any())).thenReturn(Map.of());
        when(source.projectedOutflows(any())).thenReturn(Map.of());
    }

    @Test
    void weeklyCoversTheWeeksOfTheMonth() {
        CashFlowReport report = service.weekly(new Period(2038, 3), TODAY);

        assertThat(report.lines()).hasSize(new Period(2038, 3).weeks().size());
        assertThat(report.from()).isEqualTo(LocalDate.of(2038, 3, 1));
        assertThat(report.to()).isEqualTo(LocalDate.of(2038, 3, 31));
    }

    @Test
    void monthlyHasOneLinePerMonthAndRejectsBackwardsOrTooLongRanges() {
        assertThat(service.monthly(new Period(2038, 1), new Period(2038, 3), TODAY).lines()).hasSize(3);
        assertThatThrownBy(() -> service.monthly(new Period(2038, 3), new Period(2038, 1), TODAY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.monthly(new Period(2036, 1), new Period(2038, 3), TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void projectionRunsConsecutiveWeeksAcrossMonths() {
        CashFlowReport report = service.projection(LocalDate.of(2038, 3, 29), 3, TODAY);

        assertThat(report.lines()).hasSize(3);
        assertThat(report.lines().get(0).startDate()).isEqualTo(LocalDate.of(2038, 3, 29));
        assertThat(report.lines().get(1).startDate()).isEqualTo(LocalDate.of(2038, 4, 1));
        assertThatThrownBy(() -> service.projection(TODAY, 0, TODAY)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.projection(TODAY, 53, TODAY)).isInstanceOf(IllegalArgumentException.class);
    }
}
