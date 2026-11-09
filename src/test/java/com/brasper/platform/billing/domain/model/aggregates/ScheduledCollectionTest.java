package com.brasper.platform.billing.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.billing.domain.model.commands.ScheduleCollectionCommand;
import com.brasper.platform.billing.domain.model.valueobjects.ScheduleStatus;
import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ScheduledCollectionTest {

    private static final CashFlowWeek AUG_20 = CashFlowWeek.from(LocalDate.of(2026, 8, 20));
    private static final CashFlowWeek SEP_2 = CashFlowWeek.from(LocalDate.of(2026, 9, 2));

    private static ScheduledCollection collection(String amount) {
        return new ScheduledCollection(new ScheduleCollectionCommand(3L, new Money(new BigDecimal(amount),
                Currency.USD), AUG_20, "  adelanto  "));
    }

    @Test
    void startsScheduledInTheGivenWeek() {
        ScheduledCollection collection = collection("5000");

        assertThat(collection.getStatus()).isEqualTo(ScheduleStatus.SCHEDULED);
        assertThat(collection.getExpectedWeek()).isEqualTo(AUG_20);
        assertThat(collection.getExpectedAmount()).isEqualTo(new Money(new BigDecimal("5000"), Currency.USD));
        assertThat(collection.getNotes()).isEqualTo("adelanto");
        assertThat(collection.getInvoiceId()).isNull();
    }

    @Test
    void rejectsAnAmountThatIsNotPositive() {
        assertThatThrownBy(() -> collection("0")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> collection("-1")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anOpenCollectionCanBeMovedAnyNumberOfTimes() {
        ScheduledCollection collection = collection("100");

        collection.reschedule(SEP_2);
        assertThat(collection.getStatus()).isEqualTo(ScheduleStatus.RESCHEDULED);
        assertThat(collection.getExpectedWeek()).isEqualTo(SEP_2);

        collection.reschedule(AUG_20);
        assertThat(collection.getExpectedWeek()).isEqualTo(AUG_20);
    }

    @Test
    void realizingLinksTheInvoiceAndClosesTheCollection() {
        ScheduledCollection collection = collection("100");
        collection.reschedule(SEP_2);

        collection.markAsRealized(77L);

        assertThat(collection.getStatus()).isEqualTo(ScheduleStatus.REALIZED);
        assertThat(collection.getInvoiceId()).isEqualTo(77L);
        assertThatThrownBy(() -> collection.reschedule(AUG_20)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(collection::cancel).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> collection.markAsRealized(78L)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aCancelledCollectionIsClosedToo() {
        ScheduledCollection collection = collection("100");

        collection.cancel();

        assertThat(collection.getStatus()).isEqualTo(ScheduleStatus.CANCELLED);
        assertThatThrownBy(collection::cancel).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> collection.reschedule(SEP_2)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> collection.markAsRealized(77L)).isInstanceOf(IllegalStateException.class);
    }
}
