package com.brasper.platform.staff.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.*;
import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import com.brasper.platform.staff.domain.model.aggregates.FixedCost;
import com.brasper.platform.staff.domain.model.commands.*;
import com.brasper.platform.staff.domain.model.valueobjects.*;
import com.brasper.platform.staff.domain.repositories.FixedCostRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class FixedCostJpaRepositoryTest {
    @Autowired private FixedCostRepository costs;
    @Autowired private EntityManager em;
    private FixedCost cost() {
        return new FixedCost(new CreateFixedCostCommand(new CostCode("BR01"), "salary", FixedCostType.SALARY,
                new Money(new BigDecimal("100"), Currency.PEN), 999L, new PaymentSchedule(List.of(5, 1))));
    }
    @Test void roundTripsPaymentsWeeksAndQueries() {
        FixedCost cost = costs.save(cost()); em.flush(); em.clear();
        FixedCost loaded = costs.findByCode(new CostCode("BR01")).orElseThrow();
        var period = new Period(2026, 11);
        var amount = new Money(new BigDecimal("40"), Currency.PEN);
        loaded.registerPayment(new RegisterFixedCostPaymentCommand(loaded.getId(), LocalDate.of(2026, 12, 1), period, amount, null, "November"));
        FixedCost saved = costs.save(loaded); em.flush();
        Long paymentId = saved.getPayments().getFirst().getId(); em.clear();
        FixedCost found = costs.findById(cost.getId()).orElseThrow();
        assertThat(found.getSchedule().expectedWeeks()).containsExactly(5, 1);
        assertThat(found.getPayments().getFirst()).extracting("id", "period", "amount", "notes", "bankAccountId")
                .containsExactly(paymentId, period, amount, "November", null);
        assertThat(paymentId).isNotNull();
        assertThat(found.paidIn(period)).isEqualTo(amount);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(costs.existsByCode(new CostCode("BR01"))).isTrue();
        assertThat(costs.existsByCode(new CostCode("missing"))).isFalse();
        assertThat(costs.findAllByEmployeeId(999L)).containsExactly(found);
        assertThat(costs.findAllByActive(true)).containsExactly(found);
        found.update(new UpdateFixedCostCommand(found.getId(), "updated", found.getMonthlyAmount(), new PaymentSchedule(List.of(2, 4))));
        found.deactivate(); costs.save(found); em.flush(); em.clear();
        assertThat(costs.findAllByActive(true)).isEmpty();
        assertThat(costs.findAllByActive(false)).hasSize(1);
        assertThat(costs.findById(found.getId()).orElseThrow().getSchedule().expectedWeeks()).containsExactly(2, 4);
    }
    @Test void rejectsDuplicateCode() {
        costs.save(cost()); em.flush();
        assertThatThrownBy(() -> { costs.save(cost()); em.flush(); }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
