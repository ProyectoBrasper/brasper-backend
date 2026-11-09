package com.brasper.platform.accounting.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.*;
import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.accounting.domain.model.aggregates.*;
import com.brasper.platform.accounting.domain.model.commands.*;
import com.brasper.platform.accounting.domain.model.valueobjects.TaxReturnStatus;
import com.brasper.platform.accounting.domain.repositories.*;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.*;
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
class TaxJpaRepositoryTest {
    private static final LocalDate DATE = LocalDate.of(2026, 11, 1);
    private static final Period PERIOD = new Period(2026, 11);
    private static final Money AMOUNT = new Money(new BigDecimal("123.45"), Currency.USD);
    @Autowired private TaxReturnRepository returns;
    @Autowired private TaxParametersRepository parameters;
    @Autowired private EntityManager em;
    private TaxReturn prepare() {
        return returns.save(new TaxReturn(new PrepareTaxReturnCommand(PERIOD), AMOUNT, AMOUNT, AMOUNT, AMOUNT));
    }
    @Test void roundTripsReturnAndPayment() {
        TaxReturn saved = prepare();
        em.flush(); em.clear();
        TaxReturn found = returns.findByPeriod(PERIOD).orElseThrow();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getPeriod()).isEqualTo(PERIOD);
        assertThat(found.getStatus()).isEqualTo(TaxReturnStatus.DRAFT);
        assertThat(found.getOutputIgv()).isEqualTo(AMOUNT);
        assertThat(found.getInputIgv()).isEqualTo(AMOUNT);
        assertThat(found.getCarriedCredit()).isEqualTo(AMOUNT);
        assertThat(found.getIncomeTaxPrepayment()).isEqualTo(AMOUNT);
        assertThat(found.getCreatedAt()).isNotNull().isEqualTo(found.getUpdatedAt());
        var created = found.getCreatedAt();
        found.file(); found.markAsPaid(DATE, 999L);
        em.flush(); em.clear();
        found = returns.findById(saved.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(TaxReturnStatus.PAID);
        assertThat(found.getPaidOn()).isEqualTo(DATE);
        assertThat(found.getBankAccountId()).isEqualTo(999L);
        assertThat(found.getCreatedAt()).isEqualTo(created);
        assertThat(found.getUpdatedAt()).isAfterOrEqualTo(created);
        assertThat(returns.existsByPeriod(PERIOD)).isTrue();
        assertThat(returns.existsByPeriod(PERIOD.next())).isFalse();
        assertThat(returns.findByPeriod(PERIOD.next())).isEmpty();
        assertThat(returns.findAll()).hasSize(1);
    }
    @Test void rejectsDuplicatePeriod() {
        prepare(); em.flush(); em.clear();
        assertThatThrownBy(this::prepare).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void roundTripsRatesAndFindsLatestEffectiveDate() {
        TaxParameters first = parameters.save(new TaxParameters(new SetTaxParametersCommand(TaxRates.standard(), DATE)));
        parameters.save(new TaxParameters(new SetTaxParametersCommand(TaxRates.standard(), DATE.plusDays(10))));
        em.flush(); em.clear();
        TaxParameters found = parameters.findById(first.getId()).orElseThrow();
        assertThat(found.getRates()).isEqualTo(TaxRates.standard());
        assertThat(found.getEffectiveFrom()).isEqualTo(DATE);
        assertThat(found.getCreatedAt()).isNotNull().isEqualTo(found.getUpdatedAt());
        assertThat(parameters.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(DATE.minusDays(1))).isEmpty();
        assertThat(parameters.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(DATE.plusDays(9)))
                .get().extracting(TaxParameters::getId).isEqualTo(first.getId());
        assertThat(parameters.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(DATE.plusDays(10)))
                .get().extracting(TaxParameters::getEffectiveFrom).isEqualTo(DATE.plusDays(10));
        assertThat(parameters.findAll()).hasSize(2);
    }
}
