package com.brasper.platform.expenses.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.expenses.domain.model.commands.CreateSupplierCommand;
import com.brasper.platform.expenses.domain.model.commands.UpdateSupplierCommand;
import com.brasper.platform.expenses.domain.model.valueobjects.SupplierType;
import com.brasper.platform.shared.domain.model.valueobjects.Ruc;
import org.junit.jupiter.api.Test;

class SupplierTest {

    private static final Ruc RUC = new Ruc("20123456789");

    @Test
    void createsACompanyTrimmingTextAndBlankPhoneBecomesNull() {
        Supplier supplier = new Supplier(
                new CreateSupplierCommand("  Ferreteria Sur  ", RUC, SupplierType.COMPANY, " "));

        assertThat(supplier.getBusinessName()).isEqualTo("Ferreteria Sur");
        assertThat(supplier.getRuc()).isEqualTo(RUC);
        assertThat(supplier.getType()).isEqualTo(SupplierType.COMPANY);
        assertThat(supplier.getPhone()).isNull();
    }

    @Test
    void aNaturalPersonNeedsNoRuc() {
        Supplier supplier = new Supplier(
                new CreateSupplierCommand("Juan Perez", null, SupplierType.NATURAL_PERSON, "999"));

        assertThat(supplier.getRuc()).isNull();
        assertThat(supplier.getPhone()).isEqualTo("999");
    }

    @Test
    void aCompanyWithoutRucIsRejected() {
        assertThatThrownBy(
                () -> new Supplier(new CreateSupplierCommand("Ferreteria", null, SupplierType.COMPANY, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsABlankNameAndAMissingType() {
        assertThatThrownBy(() -> new Supplier(new CreateSupplierCommand(" ", RUC, SupplierType.COMPANY, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Supplier(new CreateSupplierCommand("Ferreteria", RUC, null, null)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void updatesNameAndPhoneButNotRucOrType() {
        Supplier supplier = new Supplier(new CreateSupplierCommand("Ferreteria", RUC, SupplierType.COMPANY, "1"));

        supplier.update(new UpdateSupplierCommand(1L, "Ferreteria Norte", null));

        assertThat(supplier.getBusinessName()).isEqualTo("Ferreteria Norte");
        assertThat(supplier.getPhone()).isNull();
        assertThat(supplier.getRuc()).isEqualTo(RUC);
        assertThat(supplier.getType()).isEqualTo(SupplierType.COMPANY);
        assertThatThrownBy(() -> supplier.update(new UpdateSupplierCommand(1L, "", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
