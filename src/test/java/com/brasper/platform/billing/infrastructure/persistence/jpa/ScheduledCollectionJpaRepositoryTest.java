package com.brasper.platform.billing.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.billing.domain.model.aggregates.Invoice;
import com.brasper.platform.billing.domain.model.aggregates.ScheduledCollection;
import com.brasper.platform.billing.domain.model.commands.IssueInvoiceCommand;
import com.brasper.platform.billing.domain.model.commands.ScheduleCollectionCommand;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceNumber;
import com.brasper.platform.billing.domain.model.valueobjects.ScheduleStatus;
import com.brasper.platform.billing.domain.repositories.InvoiceRepository;
import com.brasper.platform.billing.domain.repositories.ScheduledCollectionRepository;
import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ScheduledCollectionJpaRepositoryTest {

    @Autowired
    private ScheduledCollectionRepository collections;
    @Autowired
    private InvoiceRepository invoices;
    @Autowired
    private EntityManager em;

    private static ScheduledCollection collection(LocalDate expectedOn) {
        return new ScheduledCollection(new ScheduleCollectionCommand(3L,
                new Money(new BigDecimal("5000.50"), Currency.USD), CashFlowWeek.from(expectedOn), "nota"));
    }

    @Test
    void persistsEveryFieldAndTheWeekRoundTrips() {
        Long id = collections.save(collection(LocalDate.of(2026, 8, 20))).getId();
        em.flush();
        em.clear();

        ScheduledCollection found = collections.findById(id).orElseThrow();

        assertThat(found.getProjectId()).isEqualTo(3L);
        assertThat(found.getExpectedAmount()).isEqualTo(new Money(new BigDecimal("5000.50"), Currency.USD));
        assertThat(found.getExpectedWeek()).isEqualTo(CashFlowWeek.from(LocalDate.of(2026, 8, 20)));
        assertThat(found.getStatus()).isEqualTo(ScheduleStatus.SCHEDULED);
        assertThat(found.getNotes()).isEqualTo("nota");
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void listsAMonthOrderedByWeekWhateverTheStatus() {
        collections.save(collection(LocalDate.of(2026, 8, 25)));
        ScheduledCollection early = collections.save(collection(LocalDate.of(2026, 8, 5)));
        early.cancel();
        collections.save(collection(LocalDate.of(2026, 9, 2)));
        em.flush();
        em.clear();

        var august = collections.findAllByPeriod(new Period(2026, 8));

        assertThat(august).hasSize(2);
        assertThat(august.get(0).getExpectedWeek().number()).isLessThan(august.get(1).getExpectedWeek().number());
        assertThat(august.get(0).getStatus()).isEqualTo(ScheduleStatus.CANCELLED);
        assertThat(collections.findAllByPeriod(new Period(2026, 10))).isEmpty();
    }

    @Test
    void aRealizedCollectionKeepsItsInvoice() {
        Long invoiceId = invoices.save(new Invoice(new IssueInvoiceCommand(new InvoiceNumber("F001", 7),
                LocalDate.of(2026, 8, 20), null, 3L, new Money(new BigDecimal("1000"), Currency.USD), false, false,
                false, false, null), 4L, TaxRates.standard())).getId();
        ScheduledCollection collection = collections.save(collection(LocalDate.of(2026, 8, 20)));
        collection.markAsRealized(invoiceId);
        em.flush();
        em.clear();

        ScheduledCollection found = collections.findById(collection.getId()).orElseThrow();

        assertThat(found.getStatus()).isEqualTo(ScheduleStatus.REALIZED);
        assertThat(found.getInvoiceId()).isEqualTo(invoiceId);
    }
}
