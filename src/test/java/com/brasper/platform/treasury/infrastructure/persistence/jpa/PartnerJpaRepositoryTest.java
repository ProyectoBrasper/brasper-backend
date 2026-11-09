package com.brasper.platform.treasury.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.treasury.domain.model.aggregates.Partner;
import com.brasper.platform.treasury.domain.model.commands.RegisterPartnerCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterPartnerContributionCommand;
import com.brasper.platform.treasury.domain.repositories.PartnerRepository;
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
class PartnerJpaRepositoryTest {

    @Autowired
    private PartnerRepository partners;
    @Autowired
    private EntityManager em;

    @Test
    void roundTripsPartnerAndMergedContributionsAndEnforcesUniqueNames() {
        Long id = partners.save(new Partner(new RegisterPartnerCommand(" Ana Perez ", " Engineer "))).getId();
        em.flush();
        em.clear();
        Partner partner = partners.findById(id).orElseThrow();
        assertThat(partner.getFullName()).isEqualTo("Ana Perez");
        assertThat(partner.getRole()).isEqualTo("Engineer");
        assertThat(partner.getCreatedAt()).isNotNull();
        assertThat(partner.getUpdatedAt()).isEqualTo(partner.getCreatedAt());
        assertThat(partners.existsByFullName("Ana Perez")).isTrue();
        assertThat(partners.existsByFullName("Missing")).isFalse();
        Money amount = new Money(new BigDecimal("123.45"), Currency.USD);
        partner.registerContribution(new RegisterPartnerContributionCommand(id, LocalDate.of(2026, 1, 1),
                amount, true, null, 9999L, " materials "));
        Partner saved = partners.save(partner);
        em.flush();
        Long contributionId = saved.getContributions().getFirst().getId();
        assertThat(contributionId).isNotNull();
        em.clear();
        Partner found = partners.findById(id).orElseThrow();
        assertThat(found.getContributions()).hasSize(1);
        var contribution = found.getContributions().getFirst();
        assertThat(contribution.getId()).isEqualTo(contributionId);
        assertThat(contribution.getContributedOn()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(contribution.getAmount()).isEqualTo(amount);
        assertThat(contribution.isReimbursable()).isTrue();
        assertThat(contribution.getBankAccountId()).isNull();
        assertThat(contribution.getExpenseId()).isEqualTo(9999L);
        assertThat(contribution.getConcept()).isEqualTo("materials");
        assertThatThrownBy(() -> partners.save(new Partner(new RegisterPartnerCommand(" Ana Perez ", null))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
