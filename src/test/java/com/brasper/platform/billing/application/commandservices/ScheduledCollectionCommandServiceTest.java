package com.brasper.platform.billing.application.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brasper.platform.billing.application.outboundservices.ProjectLookup;
import com.brasper.platform.billing.application.outboundservices.ProjectLookup.BillableProject;
import com.brasper.platform.billing.domain.model.aggregates.ScheduledCollection;
import com.brasper.platform.billing.domain.model.commands.CancelScheduledCollectionCommand;
import com.brasper.platform.billing.domain.model.commands.RescheduleCollectionCommand;
import com.brasper.platform.billing.domain.model.commands.ScheduleCollectionCommand;
import com.brasper.platform.billing.domain.model.valueobjects.ScheduleStatus;
import com.brasper.platform.billing.domain.repositories.ScheduledCollectionRepository;
import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ScheduledCollectionCommandServiceTest {

    private static final CashFlowWeek AUG_20 = CashFlowWeek.from(LocalDate.of(2026, 8, 20));
    private static final CashFlowWeek SEP_2 = CashFlowWeek.from(LocalDate.of(2026, 9, 2));

    private final ScheduledCollectionRepository collections = mock(ScheduledCollectionRepository.class);
    private final ProjectLookup projects = mock(ProjectLookup.class);
    private final ScheduledCollectionCommandService service =
            new ScheduledCollectionCommandService(collections, projects);

    private static ScheduleCollectionCommand schedule() {
        return new ScheduleCollectionCommand(3L, new Money(new BigDecimal("500"), Currency.PEN), AUG_20, null);
    }

    private ScheduledCollection storedCollection() {
        ScheduledCollection collection = new ScheduledCollection(schedule());
        when(collections.findById(5L)).thenReturn(Optional.of(collection));
        when(collections.save(collection)).thenReturn(collection);
        return collection;
    }

    @Test
    void schedulesForAnyExistingProjectEvenOneJustQuoted() {
        when(projects.find(3L)).thenReturn(Optional.of(new BillableProject(3L, 4L, "QUOTED")));
        when(collections.save(any(ScheduledCollection.class))).thenAnswer(call -> call.getArgument(0));

        ScheduledCollection collection = service.handle(schedule()).success().orElseThrow();

        assertThat(collection.getStatus()).isEqualTo(ScheduleStatus.SCHEDULED);
        assertThat(collection.getExpectedWeek()).isEqualTo(AUG_20);
    }

    @Test
    void rejectsAScheduledCollectionInUsd() {
        var usd = new ScheduleCollectionCommand(3L, new Money(new BigDecimal("500"), Currency.USD), AUG_20, null);

        assertThat(service.handle(usd).failure()).contains(ScheduledCollectionCommandFailure.UNSUPPORTED_CURRENCY);
        verify(projects, never()).find(any());
        verify(collections, never()).save(any());
    }

    @Test
    void schedulingForAnUnknownProjectFailsAndStoresNothing() {
        when(projects.find(3L)).thenReturn(Optional.empty());

        assertThat(service.handle(schedule()).failure())
                .contains(ScheduledCollectionCommandFailure.PROJECT_NOT_FOUND);
        verify(collections, never()).save(any());
    }

    @Test
    void reschedulesAndCancelsAnOpenCollection() {
        ScheduledCollection collection = storedCollection();

        assertThat(service.handle(new RescheduleCollectionCommand(5L, SEP_2)).isSuccess()).isTrue();
        assertThat(collection.getStatus()).isEqualTo(ScheduleStatus.RESCHEDULED);
        assertThat(collection.getExpectedWeek()).isEqualTo(SEP_2);

        assertThat(service.handle(new CancelScheduledCollectionCommand(5L)).isSuccess()).isTrue();
        assertThat(collection.getStatus()).isEqualTo(ScheduleStatus.CANCELLED);
    }

    @Test
    void aClosedCollectionCannotBeMovedOrCancelled() {
        ScheduledCollection collection = storedCollection();
        collection.markAsRealized(77L);

        assertThat(service.handle(new RescheduleCollectionCommand(5L, SEP_2)).failure())
                .contains(ScheduledCollectionCommandFailure.ALREADY_CLOSED);
        assertThat(service.handle(new CancelScheduledCollectionCommand(5L)).failure())
                .contains(ScheduledCollectionCommandFailure.ALREADY_CLOSED);
        assertThat(collection.getExpectedWeek()).isEqualTo(AUG_20);
    }

    @Test
    void changesToAnUnknownCollectionFail() {
        when(collections.findById(9L)).thenReturn(Optional.empty());

        assertThat(service.handle(new RescheduleCollectionCommand(9L, SEP_2)).failure())
                .contains(ScheduledCollectionCommandFailure.NOT_FOUND);
        assertThat(service.handle(new CancelScheduledCollectionCommand(9L)).failure())
                .contains(ScheduledCollectionCommandFailure.NOT_FOUND);
    }
}
