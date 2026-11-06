package com.brasper.platform.billing.interfaces.acl;

import static org.assertj.core.api.Assertions.assertThat;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.billing.domain.model.aggregates.Invoice;
import com.brasper.platform.billing.domain.model.aggregates.ScheduledCollection;
import com.brasper.platform.billing.domain.model.commands.IssueInvoiceCommand;
import com.brasper.platform.billing.domain.model.commands.RegisterInvoicePaymentCommand;
import com.brasper.platform.billing.domain.model.commands.ScheduleCollectionCommand;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceNumber;
import com.brasper.platform.billing.domain.model.valueobjects.InvoicePaymentType;
import com.brasper.platform.billing.domain.repositories.InvoiceRepository;
import com.brasper.platform.billing.domain.repositories.ScheduledCollectionRepository;
import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class BillingContextFacadeTest {

    @Autowired
    private BillingContextFacade facade;
    @Autowired
    private InvoiceRepository invoices;
    @Autowired
    private ScheduledCollectionRepository collections;

    private static Money pen(String amount) {
        return new Money(new BigDecimal(amount), Currency.PEN);
    }

    private Invoice invoice(int correlative, LocalDate issuedOn, Long projectId, String base) {
        return invoices.save(new Invoice(new IssueInvoiceCommand(new InvoiceNumber("F001", correlative), issuedOn,
                null, projectId, pen(base), false, false, false, false, null), 4L, TaxRates.standard()));
    }

    private void pay(Invoice invoice, String day, String amount, InvoicePaymentType type) {
        Long bank = type == InvoicePaymentType.RETENTION_CERTIFICATE ? null : 9L;
        invoice.registerPayment(new RegisterInvoicePaymentCommand(invoice.getId(), LocalDate.parse(day), pen(amount),
                type, bank, null));
        invoices.save(invoice);
    }

    private static BigDecimal n(String value) {
        return new BigDecimal(value);
    }

    /**
     * Project 3: F-1 (1180 total, 180 IGV, issued Aug 20) with 500 deposited on Sep 1 and a 100 certificate on
     * Sep 3; F-2 (590, 90 IGV, Aug 25) paid in full on Sep 2; F-3 (2360) voided. Project 4: F-4 (118, 18 IGV,
     * Sep 2). Sep 1 2026 is a Tuesday, so week 1 of September is Sep 1-6.
     */
    private void seed() {
        Invoice first = invoice(1, LocalDate.of(2026, 8, 20), 3L, "1000");
        Invoice second = invoice(2, LocalDate.of(2026, 8, 25), 3L, "500");
        invoice(3, LocalDate.of(2026, 8, 26), 3L, "2000").voidInvoice();
        invoice(4, LocalDate.of(2026, 9, 2), 4L, "100");
        pay(first, "2026-09-01", "500", InvoicePaymentType.NET_DEPOSIT);
        pay(first, "2026-09-03", "100", InvoicePaymentType.RETENTION_CERTIFICATE);
        pay(second, "2026-09-02", "590", InvoicePaymentType.NET_DEPOSIT);
    }

    @Test
    void sumsInvoicedIgvAndCollectedIgnoringVoidedInvoicesAndCertificates() {
        seed();

        assertThat(facade.sumInvoicedByProject(3L)).isEqualByComparingTo(n("1770"));
        assertThat(facade.sumCollectedByProject(3L)).isEqualByComparingTo(n("1090"));
        assertThat(facade.sumOutputIgvByPeriod(2026, 8)).isEqualByComparingTo(n("270"));
        assertThat(facade.sumOutputIgvByPeriod(2026, 9)).isEqualByComparingTo(n("18"));
        assertThat(facade.sumCollectedByWeek(2026, 9, 1)).isEqualByComparingTo(n("1090"));
        assertThat(facade.sumCollectedByWeek(2026, 9, 2)).isEqualByComparingTo(n("0"));
    }

    @Test
    void outstandingReceivablesAreAsOfTheDate() {
        seed();

        assertThat(facade.sumOutstandingReceivablesAt(LocalDate.of(2026, 8, 21))).isEqualByComparingTo(n("1180"));
        // Sep 2: F-1 owes 680, F-2 is paid, F-4 (issued that day) owes 118; the Sep 3 certificate does not count yet.
        assertThat(facade.sumOutstandingReceivablesAt(LocalDate.of(2026, 9, 2))).isEqualByComparingTo(n("798"));
        // Sep 3: the certificate settles 100 more of F-1.
        assertThat(facade.sumOutstandingReceivablesAt(LocalDate.of(2026, 9, 3))).isEqualByComparingTo(n("698"));
    }

    @Test
    void scheduledCollectionsCountOnlyOpenOnesOfThatWeek() {
        CashFlowWeek week1 = CashFlowWeek.from(LocalDate.of(2026, 9, 2));
        CashFlowWeek week2 = CashFlowWeek.from(LocalDate.of(2026, 9, 9));
        collections.save(new ScheduledCollection(new ScheduleCollectionCommand(3L, pen("300"), week1, null)));
        ScheduledCollection moved = collections.save(
                new ScheduledCollection(new ScheduleCollectionCommand(3L, pen("40"), week2, null)));
        moved.reschedule(week1);
        collections.save(moved);
        collections.save(new ScheduledCollection(new ScheduleCollectionCommand(3L, pen("50"), week2, null)));
        ScheduledCollection cancelled = collections.save(
                new ScheduledCollection(new ScheduleCollectionCommand(3L, pen("999"), week1, null)));
        cancelled.cancel();
        collections.save(cancelled);

        assertThat(facade.sumScheduledCollectionsByWeek(2026, 9, 1)).isEqualByComparingTo(n("340"));
        assertThat(facade.sumScheduledCollectionsByWeek(2026, 9, 2)).isEqualByComparingTo(n("50"));
    }

    @Test
    void emptyDataSumsToZero() {
        assertThat(facade.sumInvoicedByProject(99L)).isEqualByComparingTo(n("0"));
        assertThat(facade.sumOutstandingReceivablesAt(LocalDate.of(2026, 9, 2))).isEqualByComparingTo(n("0"));
        assertThat(facade.sumScheduledCollectionsByWeek(2026, 9, 1)).isEqualByComparingTo(n("0"));
    }
}
