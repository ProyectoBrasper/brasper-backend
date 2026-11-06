package com.brasper.platform.accounting.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.accounting.domain.model.aggregates.LedgerAccount;
import com.brasper.platform.accounting.domain.model.commands.CreateLedgerAccountCommand;
import com.brasper.platform.accounting.domain.model.valueobjects.AccountCode;
import com.brasper.platform.accounting.domain.model.valueobjects.LedgerAccountType;
import com.brasper.platform.accounting.domain.repositories.LedgerAccountRepository;
import jakarta.persistence.EntityManager;
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
class LedgerAccountJpaRepositoryTest {
    @Autowired private LedgerAccountRepository accounts;
    @Autowired private EntityManager em;

    private LedgerAccount create(String code) {
        return accounts.save(new LedgerAccount(new CreateLedgerAccountCommand(new AccountCode(code), "Cash", LedgerAccountType.ASSET)));
    }
    @Test
    void roundTripsAndQueriesAccounts() {
        LedgerAccount account = create("10");
        LedgerAccount other = create("101");
        em.flush(); em.clear();
        LedgerAccount found = accounts.findByCode(new AccountCode("10")).orElseThrow();
        assertThat(found).extracting(LedgerAccount::getId, LedgerAccount::getCode, LedgerAccount::getName,
                LedgerAccount::getType, LedgerAccount::isActive)
                .containsExactly(account.getId(), new AccountCode("10"), "Cash", LedgerAccountType.ASSET, true);
        assertThat(found.getCreatedAt()).isNotNull().isEqualTo(found.getUpdatedAt());
        var createdAt = found.getCreatedAt();
        found.deactivate();
        em.flush(); em.clear();
        LedgerAccount updated = accounts.findById(found.getId()).orElseThrow();
        assertThat(updated.isActive()).isFalse();
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(createdAt);
        assertThat(accounts.existsByCode(new AccountCode("10"))).isTrue();
        assertThat(accounts.existsByCode(new AccountCode("99"))).isFalse();
        assertThat(accounts.findByCode(new AccountCode("99"))).isEmpty();
        assertThat(accounts.findAll()).hasSize(2);
        assertThat(accounts.findAllByCodeIn(List.of(new AccountCode("10"), new AccountCode("99"))))
                .extracting(LedgerAccount::getId).containsExactly(account.getId());
        assertThat(accounts.findAllByCodeIn(List.of(new AccountCode("10"), new AccountCode("101"))))
                .extracting(LedgerAccount::getId).containsExactlyInAnyOrder(account.getId(), other.getId());
        assertThat(accounts.findAllByCodeIn(List.of())).isEmpty();
    }
    @Test
    void rejectsDuplicateCode() {
        create("10");
        em.flush(); em.clear();
        assertThatThrownBy(() -> { create("10"); em.flush(); })
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
