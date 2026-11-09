package com.brasper.platform.accounting.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.accounting.application.queryservices.LedgerQueryService;
import com.brasper.platform.accounting.domain.model.aggregates.JournalEntry;
import com.brasper.platform.accounting.domain.model.valueobjects.EntrySource;
import com.brasper.platform.billing.domain.events.InvoiceIssuedEvent;
import com.brasper.platform.projects.domain.events.ProjectCompletedEvent;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

/** Not @Transactional on purpose: the handlers run when the publishing transaction commits. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ProjectsPostingEventHandlerTest {

    private static final LocalDate DAY = LocalDate.of(2039, 9, 10);

    @Autowired private ApplicationEventPublisher publisher;
    @Autowired private TransactionTemplate transaction;
    @Autowired private LedgerQueryService ledger;

    private static Money pen(String amount) {
        return new Money(new BigDecimal(amount), Currency.PEN);
    }

    private void publish(Object event) {
        transaction.executeWithoutResult(status -> publisher.publishEvent(event));
    }

    private List<JournalEntry> completionEntries(Long projectId) {
        return ledger.findEntriesByPeriod(new Period(2039, 9)).stream()
                .filter(e -> e.getSource() == EntrySource.PROJECT_COMPLETED && projectId.equals(e.getSourceId())).toList();
    }

    @Test
    void recognizesTheAdvancesOfTheProjectOnceWhenItCompletes() {
        publish(new InvoiceIssuedEvent(9601L, 9501L, DAY, pen("1000"), pen("180"), pen("1180"), true));
        publish(new InvoiceIssuedEvent(9602L, 9501L, DAY, pen("200"), pen("36"), pen("236"), true));
        publish(new InvoiceIssuedEvent(9603L, 9501L, DAY, pen("500"), pen("90"), pen("590"), false));

        publish(new ProjectCompletedEvent(9501L, DAY));
        publish(new ProjectCompletedEvent(9501L, DAY));

        List<JournalEntry> entries = completionEntries(9501L);
        assertThat(entries).hasSize(1);
        BigDecimal debit = transaction.execute(status ->
                completionEntries(9501L).getFirst().totalDebit().amount());
        assertThat(debit).isEqualByComparingTo("1200");
    }

    @Test
    void aProjectWithoutAdvancesPostsNothing() {
        publish(new InvoiceIssuedEvent(9611L, 9502L, DAY, pen("500"), pen("90"), pen("590"), false));

        publish(new ProjectCompletedEvent(9502L, DAY));

        assertThat(completionEntries(9502L)).isEmpty();
    }
}
