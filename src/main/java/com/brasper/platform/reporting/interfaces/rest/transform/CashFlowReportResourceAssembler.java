package com.brasper.platform.reporting.interfaces.rest.transform;

import com.brasper.platform.reporting.domain.model.readmodels.CashFlowLine;
import com.brasper.platform.reporting.domain.model.readmodels.CashFlowReport;
import com.brasper.platform.reporting.domain.model.valueobjects.CashFlowConcept;
import com.brasper.platform.reporting.interfaces.rest.resources.CashFlowReportResource;
import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

public final class CashFlowReportResourceAssembler {

    private CashFlowReportResourceAssembler() { }

    public static CashFlowReportResource toResource(CashFlowReport report) {
        return new CashFlowReportResource(report.from(), report.to(), report.finalBalance(),
                report.hasNegativeBalance(), report.lines().stream().map(CashFlowReportResourceAssembler::toLine).toList());
    }

    private static CashFlowReportResource.Line toLine(CashFlowLine line) {
        return new CashFlowReportResource.Line(line.label(), line.startDate(), line.endDate(), line.projected(),
                line.openingBalance(), names(line.inflows()), names(line.outflows()), line.totalInflows(),
                line.totalOutflows(), line.netFlow(), line.closingBalance());
    }

    private static Map<String, BigDecimal> names(Map<CashFlowConcept, BigDecimal> amounts) {
        Map<String, BigDecimal> byName = new TreeMap<>();
        amounts.forEach((concept, amount) -> byName.put(concept.name(), amount));
        return byName;
    }
}
