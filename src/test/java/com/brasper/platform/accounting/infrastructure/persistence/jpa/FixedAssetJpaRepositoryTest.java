package com.brasper.platform.accounting.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.accounting.domain.model.aggregates.FixedAsset;
import com.brasper.platform.accounting.domain.model.commands.RegisterFixedAssetCommand;
import com.brasper.platform.accounting.domain.repositories.FixedAssetRepository;
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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class FixedAssetJpaRepositoryTest {
    @Autowired private FixedAssetRepository assets;
    @Autowired private EntityManager em;

    @Test
    void roundTripsAmountsDatesAndOptionalExpense() {
        LocalDate date = LocalDate.of(2026, 1, 31);
        Money base = new Money(new BigDecimal("1000.01"), Currency.USD);
        FixedAsset saved = assets.save(new FixedAsset(new RegisterFixedAssetCommand("Machine", date, base, 3, 42L), TaxRates.standard()));
        assets.save(new FixedAsset(new RegisterFixedAssetCommand("Desk", date, base, 600, null), TaxRates.standard()));
        em.flush(); em.clear();
        FixedAsset found = assets.findById(saved.getId()).orElseThrow();
        assertThat(found).extracting(FixedAsset::getDescription, FixedAsset::getAcquiredOn, FixedAsset::getAmounts,
                FixedAsset::getUsefulLifeMonths, FixedAsset::getExpenseId, FixedAsset::getDisposedOn)
                .containsExactly("Machine", date, saved.getAmounts(), 3, 42L, null);
        assertThat(found.getAmounts().taxableBase()).isEqualTo(base);
        assertThat(found.getCreatedAt()).isNotNull().isEqualTo(found.getUpdatedAt());
        var createdAt = found.getCreatedAt();
        found.dispose(date.plusMonths(1));
        em.flush(); em.clear();
        FixedAsset disposed = assets.findById(saved.getId()).orElseThrow();
        assertThat(disposed.getDisposedOn()).isEqualTo(date.plusMonths(1));
        assertThat(disposed.getCreatedAt()).isEqualTo(createdAt);
        assertThat(disposed.getUpdatedAt()).isAfterOrEqualTo(createdAt);
        assertThat(disposed.accumulatedDepreciationAt(date.plusYears(10))).isEqualTo(disposed.monthlyDepreciation());
        assertThat(assets.findAll()).hasSize(2).extracting(FixedAsset::getExpenseId).containsExactlyInAnyOrder(42L, null);
    }
}
