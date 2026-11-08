package com.brasper.platform.treasury.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.PaymentStatus;
import com.brasper.platform.treasury.domain.model.aggregates.Payable;
import com.brasper.platform.treasury.domain.model.commands.CreatePayableCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterPayablePaymentCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.PayableType;
import com.brasper.platform.treasury.domain.repositories.PayableRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
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
class PayableJpaRepositoryTest {
    @Autowired
    private PayableRepository payables;
    @Autowired
    private EntityManager em;

    @Test
    void roundTripsTheAggregateAndAPaymentAddedAfterLoading() {
        Long bankId = ((Number) em.createNativeQuery("""
                insert into bank_accounts (bank, account_number, type, currency, opening_balance, opened_on, created_at, updated_at)
                values ('BCP', 'payables-test', 'CHECKING', 'USD', 0, current_date, now(), now()) returning id
                """, Long.class).getSingleResult()).longValue();
        Money total = new Money(new BigDecimal("123.45"), Currency.USD);
        CashFlowWeek week = new CashFlowWeek(2026, 11, 6);
        Long id = payables.save(new Payable(new CreatePayableCommand(" materials ", PayableType.SUPPLIER,
                total, 9997L, 9998L, 9999L, week))).getId();
        em.flush();
        em.clear();
        Payable found = payables.findByExpenseId(9999L).orElseThrow();
        assertThat(found).extracting(Payable::getId, Payable::getConcept, Payable::getType, Payable::getTotalAmount,
                Payable::getSupplierId, Payable::getProjectId, Payable::getExpenseId, Payable::getScheduledWeek,
                Payable::getStatus).containsExactly(id, "materials", PayableType.SUPPLIER, total,
                9997L, 9998L, 9999L, week, PaymentStatus.PENDING);
        assertThat(found.getCreatedAt()).isNotNull().isEqualTo(found.getUpdatedAt());
        LocalDate paidOn = LocalDate.of(2026, 11, 30);
        found.registerPayment(new RegisterPayablePaymentCommand(id, paidOn, total, bankId));
        Payable saved = payables.save(found);
        em.flush();
        Long paymentId = saved.getPayments().getFirst().getId();
        assertThat(paymentId).isNotNull();
        em.clear();
        Payable reloaded = payables.findById(id).orElseThrow();
        assertThat(reloaded).extracting(Payable::getStatus, Payable::paidAmount, Payable::balance)
                .containsExactly(PaymentStatus.PAID, total, Money.zero(Currency.USD));
        assertThat(reloaded.getPayments()).hasSize(1);
        assertThat(reloaded.getPayments().getFirst()).extracting("id", "amount", "paidOn", "bankAccountId")
                .containsExactly(paymentId, total, paidOn, bankId);
    }

    @Test
    void aStalePayableCannotBeSavedOverAConcurrentChange() {
        Long bankId = ((Number) em.createNativeQuery("""
                insert into bank_accounts (bank, account_number, type, currency, opening_balance, opened_on, created_at, updated_at)
                values ('BCP', 'payables-lock-test', 'CHECKING', 'USD', 0, current_date, now(), now()) returning id
                """, Long.class).getSingleResult()).longValue();
        Money total = new Money(new BigDecimal("50.00"), Currency.USD);
        Long id = payables.save(new Payable(new CreatePayableCommand("stale", PayableType.OTHER,
                total, null, null, null, new CashFlowWeek(2026, 11, 6)))).getId();
        em.flush();
        Payable stale = payables.findById(id).orElseThrow();
        // Another transaction already changed the row (version 0 -> 1).
        em.createNativeQuery("update payables set version = version + 1 where id = :id")
                .setParameter("id", id).executeUpdate();
        stale.registerPayment(new RegisterPayablePaymentCommand(id, LocalDate.of(2026, 11, 30), total, bankId));
        assertThatThrownBy(em::flush).isInstanceOf(OptimisticLockException.class);
    }
}
