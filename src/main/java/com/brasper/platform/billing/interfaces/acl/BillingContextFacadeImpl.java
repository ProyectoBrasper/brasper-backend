package com.brasper.platform.billing.interfaces.acl;

import com.brasper.platform.billing.application.queryservices.InvoiceQueryService;
import com.brasper.platform.billing.application.queryservices.ScheduledCollectionQueryService;
import com.brasper.platform.billing.domain.model.aggregates.Invoice;
import com.brasper.platform.billing.domain.model.entities.InvoicePayment;
import com.brasper.platform.billing.domain.model.valueobjects.InvoicePaymentType;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceStatus;
import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

/**
 * Lives in {@code interfaces} (the UML puts it in {@code application}) because the layer rules forbid
 * application from depending on interfaces, where the facade contract lives.
 */
@Service
public class BillingContextFacadeImpl implements BillingContextFacade {

    private final InvoiceQueryService invoices;
    private final ScheduledCollectionQueryService collections;

    public BillingContextFacadeImpl(InvoiceQueryService invoices, ScheduledCollectionQueryService collections) {
        this.invoices = invoices;
        this.collections = collections;
    }

    @Override
    public BigDecimal sumOutstandingReceivablesAt(LocalDate date) {
        return sum(liveInvoices(null).filter(invoice -> !invoice.getIssuedOn().isAfter(date))
                .map(invoice -> invoice.getTotal().amount().subtract(
                        sum(invoice.getPayments().stream().filter(payment -> !payment.getReceivedOn().isAfter(date))
                                .map(payment -> payment.getAmount().amount())))));
    }

    @Override
    public BigDecimal sumOutputIgvByPeriod(int year, int month) {
        Period period = new Period(year, month);
        return invoices.sumIgvIssued(period.startDate(), period.endDate());
    }

    @Override
    public BigDecimal sumInvoicedByProject(Long projectId) {
        return sum(liveInvoices(projectId).map(invoice -> invoice.getTotal().amount()));
    }

    @Override
    public BigDecimal sumCollectedByProject(Long projectId) {
        return sumDeposits(liveInvoices(projectId), payment -> true);
    }

    @Override
    public BigDecimal sumCollectedByWeek(int year, int month, int week) {
        CashFlowWeek cashFlowWeek = new CashFlowWeek(year, month, week);
        return invoices.sumDepositsReceived(cashFlowWeek.startDate(), cashFlowWeek.endDate());
    }

    @Override
    public BigDecimal sumScheduledCollectionsByWeek(int year, int month, int week) {
        CashFlowWeek cashFlowWeek = new CashFlowWeek(year, month, week);
        return sum(collections.findByPeriod(cashFlowWeek.period()).stream()
                .filter(collection -> collection.getStatus().isOpen())
                .filter(collection -> collection.getExpectedWeek().equals(cashFlowWeek))
                .map(collection -> collection.getExpectedAmount().amount()));
    }

    private Stream<Invoice> liveInvoices(Long projectId) {
        return invoices.findAll(null, projectId, null).stream()
                .filter(invoice -> invoice.getStatus() != InvoiceStatus.VOIDED);
    }

    private static BigDecimal sumDeposits(Stream<Invoice> invoices, Predicate<InvoicePayment> filter) {
        return sum(invoices.flatMap(invoice -> invoice.getPayments().stream())
                .filter(payment -> payment.getType() != InvoicePaymentType.RETENTION_CERTIFICATE)
                .filter(filter)
                .map(payment -> payment.getAmount().amount()));
    }

    private static BigDecimal sum(Stream<BigDecimal> amounts) {
        return amounts.reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
