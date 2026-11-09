package com.brasper.platform.billing.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.billing.domain.model.aggregates.Invoice;
import com.brasper.platform.billing.domain.model.commands.IssueInvoiceCommand;
import com.brasper.platform.billing.domain.model.commands.RegisterInvoicePaymentCommand;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceNumber;
import com.brasper.platform.billing.domain.model.valueobjects.InvoicePaymentType;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceStatus;
import com.brasper.platform.billing.domain.repositories.InvoiceRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class InvoiceJpaRepositoryTest {

    @Autowired
    private InvoiceRepository invoices;
    @Autowired
    private EntityManager em;

    private static Invoice invoice(String series, int correlative) {
        return new Invoice(new IssueInvoiceCommand(new InvoiceNumber(series, correlative), LocalDate.of(2026, 8, 20),
                "OC-77", 3L, new Money(new BigDecimal("1000"), Currency.USD), true, true, true, true, "nota"),
                4L, TaxRates.standard());
    }

    private static Invoice invoiceOn(String series, int correlative, long projectId, LocalDate issuedOn) {
        return new Invoice(new IssueInvoiceCommand(new InvoiceNumber(series, correlative), issuedOn, null, projectId,
                new Money(new BigDecimal("100"), Currency.PEN), false, false, false, false, null),
                4L, TaxRates.standard());
    }

    private Invoice saveAndFlush(Invoice invoice) {
        Invoice saved = invoices.save(invoice);
        em.flush();
        return saved;
    }

    @Test
    void persistsEveryFieldAndFindsTheNumber() {
        Long id = saveAndFlush(invoice("F001", 7)).getId();
        em.clear();

        Invoice found = invoices.findById(id).orElseThrow();

        assertThat(found.getNumber()).isEqualTo(new InvoiceNumber("F001", 7));
        assertThat(found.getProjectId()).isEqualTo(3L);
        assertThat(found.getClientId()).isEqualTo(4L);
        assertThat(found.getPurchaseOrder()).isEqualTo("OC-77");
        assertThat(found.getAmounts().total()).isEqualTo(new Money(new BigDecimal("1180"), Currency.USD));
        assertThat(found.getRetention().amount()).isEqualByComparingTo("35.40");
        assertThat(found.getDetraction().amount()).isEqualByComparingTo("142");
        assertThat(found.isSelfDetraction()).isTrue();
        assertThat(found.isAdvance()).isTrue();
        assertThat(found.getStatus()).isEqualTo(InvoiceStatus.PENDING);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(invoices.existsByNumber(new InvoiceNumber("f001", 7))).isTrue();
        assertThat(invoices.existsByNumber(new InvoiceNumber("F001", 8))).isFalse();
    }

    @Test
    void paymentsArePersistedWithTheInvoiceAndKeepTheStatus() {
        Invoice invoice = invoice("F001", 7);
        invoice.registerPayment(new RegisterInvoicePaymentCommand(null, LocalDate.of(2026, 9, 1),
                new Money(new BigDecimal("500"), Currency.USD), InvoicePaymentType.NET_DEPOSIT, 9L, "OP-1"));
        invoice.registerPayment(new RegisterInvoicePaymentCommand(null, LocalDate.of(2026, 9, 2),
                new Money(new BigDecimal("35.40"), Currency.USD), InvoicePaymentType.RETENTION_CERTIFICATE, null,
                null));
        Long id = saveAndFlush(invoice).getId();
        em.clear();

        Invoice found = invoices.findById(id).orElseThrow();

        assertThat(found.getPayments()).hasSize(2);
        assertThat(found.collectedAmount().amount()).isEqualByComparingTo("535.40");
        assertThat(found.outstandingBalance().amount()).isEqualByComparingTo("644.60");
        assertThat(found.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_COLLECTED);
        assertThat(found.getPayments()).extracting(p -> p.getType())
                .containsExactlyInAnyOrder(InvoicePaymentType.NET_DEPOSIT, InvoicePaymentType.RETENTION_CERTIFICATE);
    }

    @Test
    void filtersAndPagesInTheQueryNewestFirst() {
        long project = 987_654L;
        Invoice august = saveAndFlush(invoiceOn("F001", 1, project, LocalDate.of(2026, 8, 20)));
        Invoice septemberA = saveAndFlush(invoiceOn("F001", 2, project, LocalDate.of(2026, 9, 1)));
        Invoice septemberB = saveAndFlush(invoiceOn("F001", 3, project, LocalDate.of(2026, 9, 30)));
        saveAndFlush(invoiceOn("F001", 4, project + 1, LocalDate.of(2026, 9, 15)));
        septemberA.voidInvoice();
        em.flush();

        LocalDate first = LocalDate.of(1900, 1, 1);
        LocalDate last = LocalDate.of(9999, 12, 31);
        assertThat(invoices.filter(null, project, first, last, 0, 10)).containsExactly(septemberB, septemberA, august);
        assertThat(invoices.filter(null, project, first, last, 1, 2)).containsExactly(august);
        assertThat(invoices.countFiltered(null, project, first, last)).isEqualTo(3);
        assertThat(invoices.filter(InvoiceStatus.PENDING, project, first, last, 0, 10))
                .containsExactly(septemberB, august);
        assertThat(invoices.filter(null, project, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), 0, 10))
                .containsExactly(septemberB, septemberA);
        assertThat(invoices.countFiltered(InvoiceStatus.VOIDED, project, LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30))).isEqualTo(1);
    }

    @Test
    void sumsDepositsAndIgvInTheQueryLeavingOutVoidedInvoicesAndRetentionCertificates() {
        LocalDate from = LocalDate.of(2031, 3, 1);
        LocalDate to = LocalDate.of(2031, 3, 31);
        assertThat(invoices.sumDepositsReceived(from, to)).isEqualByComparingTo("0");
        assertThat(invoices.sumIgvIssued(from, to)).isEqualByComparingTo("0");

        Invoice live = saveAndFlush(invoiceOn("F009", 1, 987_655L, LocalDate.of(2031, 3, 10)));
        live.registerPayment(deposit(LocalDate.of(2031, 3, 15), "40", InvoicePaymentType.NET_DEPOSIT));
        live.registerPayment(deposit(LocalDate.of(2031, 4, 1), "10", InvoicePaymentType.NET_DEPOSIT));
        live.registerPayment(deposit(LocalDate.of(2031, 3, 20), "5", InvoicePaymentType.RETENTION_CERTIFICATE));
        Invoice voided = saveAndFlush(invoiceOn("F009", 2, 987_655L, LocalDate.of(2031, 3, 12)));
        voided.voidInvoice();
        saveAndFlush(invoiceOn("F009", 3, 987_655L, LocalDate.of(2031, 4, 2)));
        em.flush();

        assertThat(invoices.sumDepositsReceived(from, to)).isEqualByComparingTo("40");
        assertThat(invoices.sumDepositsReceived(LocalDate.of(2031, 3, 15), LocalDate.of(2031, 4, 1)))
                .isEqualByComparingTo("50");
        assertThat(invoices.sumIgvIssued(from, to)).isEqualByComparingTo("18");
    }

    private static RegisterInvoicePaymentCommand deposit(LocalDate on, String amount, InvoicePaymentType type) {
        return new RegisterInvoicePaymentCommand(null, on, new Money(new BigDecimal(amount), Currency.PEN), type,
                type == InvoicePaymentType.RETENTION_CERTIFICATE ? null : 9L, null);
    }

    @Test
    void theNumberIsUniqueButTheSameCorrelativeInAnotherSeriesIsFine() {
        saveAndFlush(invoice("F001", 7));
        saveAndFlush(invoice("F002", 7));

        assertThatThrownBy(() -> saveAndFlush(invoice("F001", 7)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
