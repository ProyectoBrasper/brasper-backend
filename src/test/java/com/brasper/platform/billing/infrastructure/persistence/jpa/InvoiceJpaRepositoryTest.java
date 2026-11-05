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
                "OC-77", 3L, 4L, new Money(new BigDecimal("1000"), Currency.USD), true, true, true, true, "nota"),
                TaxRates.standard());
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
    void theNumberIsUniqueButTheSameCorrelativeInAnotherSeriesIsFine() {
        saveAndFlush(invoice("F001", 7));
        saveAndFlush(invoice("F002", 7));

        assertThatThrownBy(() -> saveAndFlush(invoice("F001", 7)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
