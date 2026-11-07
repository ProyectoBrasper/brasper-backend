package com.brasper.platform.reporting.infrastructure.acl;

import com.brasper.platform.billing.interfaces.acl.BillingContextFacade;
import com.brasper.platform.reporting.domain.model.valueobjects.CashFlowConcept;
import com.brasper.platform.reporting.domain.services.CashFlowSource;
import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.staff.interfaces.acl.StaffContextFacade;
import com.brasper.platform.treasury.interfaces.acl.TreasuryContextFacade;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * What happened comes from Treasury's cash movements (the real money) plus the fixed cost payments of Staff, which
 * carry the cost type the movements lack. What is scheduled comes from Billing (collections), Treasury (payables and
 * loan installments) and Staff (fixed costs).
 * shortcut: a fixed cost payment that names no bank account counts here but never reached the cash balance, so the
 * chained balance can drift from the real one by those payments.
 */
@Component
class ReportingFacadeCashFlowSource implements CashFlowSource {

    private final TreasuryContextFacade treasury;
    private final StaffContextFacade staff;
    private final BillingContextFacade billing;

    ReportingFacadeCashFlowSource(TreasuryContextFacade treasury, StaffContextFacade staff, BillingContextFacade billing) {
        this.treasury = treasury;
        this.staff = staff;
        this.billing = billing;
    }

    @Override
    public BigDecimal cashBalanceAt(LocalDate date) {
        return treasury.fetchCashBalanceAt(date);
    }

    @Override
    public Map<CashFlowConcept, BigDecimal> actualInflows(CashFlowWeek week) {
        Map<CashFlowConcept, BigDecimal> flows = new EnumMap<>(CashFlowConcept.class);
        treasury.fetchMovementsBetween(week.startDate(), week.endDate()).forEach((key, amount) -> {
            if (key.startsWith("INFLOW:")) {
                flows.merge(inflowConcept(key.substring("INFLOW:".length())), amount, BigDecimal::add);
            }
        });
        return flows;
    }

    @Override
    public Map<CashFlowConcept, BigDecimal> actualOutflows(CashFlowWeek week) {
        Map<CashFlowConcept, BigDecimal> flows = new EnumMap<>(CashFlowConcept.class);
        treasury.fetchMovementsBetween(week.startDate(), week.endDate()).forEach((key, amount) -> {
            if (key.startsWith("OUTFLOW:") && !key.equals("OUTFLOW:FIXED_COST_PAYMENT")) {
                flows.merge(outflowConcept(key.substring("OUTFLOW:".length())), amount, BigDecimal::add);
            }
        });
        addFixedCosts(flows, staff.fetchFixedCostsPaidByWeek(week.year(), week.month(), week.number()));
        return flows;
    }

    @Override
    public Map<CashFlowConcept, BigDecimal> projectedInflows(CashFlowWeek week) {
        Map<CashFlowConcept, BigDecimal> flows = new EnumMap<>(CashFlowConcept.class);
        flows.merge(CashFlowConcept.INVOICE_COLLECTIONS,
                billing.sumScheduledCollectionsByWeek(week.year(), week.month(), week.number()), BigDecimal::add);
        flows.merge(CashFlowConcept.OTHER_INCOME,
                treasury.sumScheduledInstallmentsByWeek(week.year(), week.month(), week.number())
                        .getOrDefault("GRANTED", BigDecimal.ZERO), BigDecimal::add);
        return withoutZeros(flows);
    }

    @Override
    public Map<CashFlowConcept, BigDecimal> projectedOutflows(CashFlowWeek week) {
        Map<CashFlowConcept, BigDecimal> flows = new EnumMap<>(CashFlowConcept.class);
        flows.merge(CashFlowConcept.PENDING_PAYABLES,
                treasury.sumScheduledPayablesByWeek(week.year(), week.month(), week.number()), BigDecimal::add);
        flows.merge(CashFlowConcept.LOAN_REPAYMENTS,
                treasury.sumScheduledInstallmentsByWeek(week.year(), week.month(), week.number())
                        .getOrDefault("RECEIVED", BigDecimal.ZERO), BigDecimal::add);
        addFixedCosts(flows, staff.fetchFixedCostsScheduledByWeek(week.year(), week.month(), week.number()));
        return withoutZeros(flows);
    }

    private static CashFlowConcept inflowConcept(String source) {
        return switch (source) {
            case "INVOICE_PAYMENT" -> CashFlowConcept.INVOICE_COLLECTIONS;
            case "LOAN_DISBURSEMENT" -> CashFlowConcept.LOANS;
            case "PARTNER_CONTRIBUTION" -> CashFlowConcept.PARTNER_CONTRIBUTIONS;
            default -> CashFlowConcept.OTHER_INCOME;
        };
    }

    private static CashFlowConcept outflowConcept(String source) {
        return switch (source) {
            case "EXPENSE" -> CashFlowConcept.SUPPLIERS;
            case "PAYABLE_PAYMENT" -> CashFlowConcept.PENDING_PAYABLES;
            case "LOAN_INSTALLMENT" -> CashFlowConcept.LOAN_REPAYMENTS;
            case "TAX_PAYMENT" -> CashFlowConcept.TAXES;
            default -> CashFlowConcept.OTHER_EXPENSES;
        };
    }

    private static void addFixedCosts(Map<CashFlowConcept, BigDecimal> flows, Map<String, BigDecimal> byType) {
        byType.forEach((type, amount) -> flows.merge(switch (type) {
            case "SALARY", "OVERTIME" -> CashFlowConcept.PAYROLL;
            case "OFFICE_RENT" -> CashFlowConcept.OFFICE_RENT;
            case "CLEANING" -> CashFlowConcept.CLEANING;
            case "SCTR_INSURANCE", "LIFE_INSURANCE" -> CashFlowConcept.INSURANCE;
            default -> CashFlowConcept.OTHER_EXPENSES;
        }, amount, BigDecimal::add));
    }

    private static Map<CashFlowConcept, BigDecimal> withoutZeros(Map<CashFlowConcept, BigDecimal> flows) {
        flows.values().removeIf(amount -> amount.signum() == 0);
        return flows;
    }
}
