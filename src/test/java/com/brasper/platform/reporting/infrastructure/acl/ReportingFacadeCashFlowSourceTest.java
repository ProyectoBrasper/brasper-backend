package com.brasper.platform.reporting.infrastructure.acl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.brasper.platform.billing.interfaces.acl.BillingContextFacade;
import com.brasper.platform.reporting.domain.model.valueobjects.CashFlowConcept;
import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.staff.interfaces.acl.StaffContextFacade;
import com.brasper.platform.treasury.interfaces.acl.TreasuryContextFacade;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReportingFacadeCashFlowSourceTest {

    private static final CashFlowWeek WEEK = new CashFlowWeek(2038, 3, 2);

    private final TreasuryContextFacade treasury = mock(TreasuryContextFacade.class);
    private final StaffContextFacade staff = mock(StaffContextFacade.class);
    private final BillingContextFacade billing = mock(BillingContextFacade.class);
    private final ReportingFacadeCashFlowSource source = new ReportingFacadeCashFlowSource(treasury, staff, billing);

    private static BigDecimal n(String value) {
        return new BigDecimal(value);
    }

    @Test
    void mapsTheRealMovementsToConceptsAndTakesFixedCostsFromStaff() {
        when(treasury.fetchMovementsBetween(LocalDate.of(2038, 3, 8), LocalDate.of(2038, 3, 14))).thenReturn(Map.of(
                "INFLOW:INVOICE_PAYMENT", n("500"), "INFLOW:LOAN_DISBURSEMENT", n("1000"), "INFLOW:ADJUSTMENT", n("5"),
                "OUTFLOW:EXPENSE", n("200"), "OUTFLOW:PAYABLE_PAYMENT", n("80"), "OUTFLOW:TAX_PAYMENT", n("19"),
                "OUTFLOW:FIXED_COST_PAYMENT", n("999")));
        when(staff.fetchFixedCostsPaidByWeek(2038, 3, 2)).thenReturn(Map.of("SALARY", n("300"), "OFFICE_RENT", n("100")));

        var inflows = source.actualInflows(WEEK);
        var outflows = source.actualOutflows(WEEK);

        assertThat(inflows).containsEntry(CashFlowConcept.INVOICE_COLLECTIONS, n("500"))
                .containsEntry(CashFlowConcept.LOANS, n("1000")).containsEntry(CashFlowConcept.OTHER_INCOME, n("5"));
        assertThat(outflows).containsEntry(CashFlowConcept.SUPPLIERS, n("200"))
                .containsEntry(CashFlowConcept.PENDING_PAYABLES, n("80")).containsEntry(CashFlowConcept.TAXES, n("19"))
                .containsEntry(CashFlowConcept.PAYROLL, n("300")).containsEntry(CashFlowConcept.OFFICE_RENT, n("100"))
                .doesNotContainValue(n("999"));
    }

    @Test
    void composesTheScheduledFlowsFromBillingTreasuryAndStaff() {
        when(billing.sumScheduledCollectionsByWeek(2038, 3, 2)).thenReturn(n("700"));
        when(treasury.sumScheduledPayablesByWeek(2038, 3, 2)).thenReturn(n("90"));
        when(treasury.sumScheduledInstallmentsByWeek(2038, 3, 2)).thenReturn(Map.of("RECEIVED", n("450"), "GRANTED", n("60")));
        when(staff.fetchFixedCostsScheduledByWeek(2038, 3, 2)).thenReturn(Map.of("SCTR_INSURANCE", n("40"), "HOSTING", n("15")));

        assertThat(source.projectedInflows(WEEK)).containsEntry(CashFlowConcept.INVOICE_COLLECTIONS, n("700"))
                .containsEntry(CashFlowConcept.OTHER_INCOME, n("60"));
        assertThat(source.projectedOutflows(WEEK)).containsEntry(CashFlowConcept.PENDING_PAYABLES, n("90"))
                .containsEntry(CashFlowConcept.LOAN_REPAYMENTS, n("450")).containsEntry(CashFlowConcept.INSURANCE, n("40"))
                .containsEntry(CashFlowConcept.OTHER_EXPENSES, n("15"));
    }

    @Test
    void leavesOutConceptsWithNothingScheduled() {
        when(billing.sumScheduledCollectionsByWeek(2038, 3, 2)).thenReturn(BigDecimal.ZERO);
        when(treasury.sumScheduledInstallmentsByWeek(2038, 3, 2)).thenReturn(Map.of());
        when(treasury.sumScheduledPayablesByWeek(2038, 3, 2)).thenReturn(BigDecimal.ZERO);
        when(staff.fetchFixedCostsScheduledByWeek(2038, 3, 2)).thenReturn(Map.of());

        assertThat(source.projectedInflows(WEEK)).isEmpty();
        assertThat(source.projectedOutflows(WEEK)).isEmpty();
    }
}
