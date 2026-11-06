package com.brasper.platform.expenses.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.expenses.domain.model.aggregates.Supplier;
import com.brasper.platform.expenses.domain.model.commands.CreateSupplierCommand;
import com.brasper.platform.expenses.domain.model.valueobjects.SupplierType;
import com.brasper.platform.expenses.domain.repositories.SupplierRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Ruc;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class SupplierJpaRepositoryTest {

    private static final Ruc RUC = new Ruc("20123456789");

    @Autowired
    private SupplierRepository suppliers;
    @Autowired
    private EntityManager em;

    private static Supplier company(String name, Ruc ruc) {
        return new Supplier(new CreateSupplierCommand(name, ruc, SupplierType.COMPANY, "999"));
    }

    @Test
    void persistsEveryFieldAndFindsByRuc() {
        Long id = suppliers.save(company("Ferreteria Sur", RUC)).getId();
        em.flush();
        em.clear();

        Supplier found = suppliers.findById(id).orElseThrow();

        assertThat(found.getBusinessName()).isEqualTo("Ferreteria Sur");
        assertThat(found.getRuc()).isEqualTo(RUC);
        assertThat(found.getType()).isEqualTo(SupplierType.COMPANY);
        assertThat(found.getPhone()).isEqualTo("999");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(suppliers.existsByRuc(RUC)).isTrue();
        assertThat(suppliers.existsByRuc(new Ruc("20999999999"))).isFalse();
    }

    @Test
    void severalNaturalPersonsMayHaveNoRuc() {
        suppliers.save(new Supplier(new CreateSupplierCommand("Juan", null, SupplierType.NATURAL_PERSON, null)));
        suppliers.save(new Supplier(new CreateSupplierCommand("Pedro", null, SupplierType.NATURAL_PERSON, null)));
        em.flush();

        assertThat(suppliers.findAll()).hasSize(2);
    }

    @Test
    void theDatabaseRejectsADuplicateRuc() {
        suppliers.save(company("Una", RUC));
        em.flush();

        assertThatThrownBy(() -> {
            suppliers.save(company("Otra", RUC));
            em.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
