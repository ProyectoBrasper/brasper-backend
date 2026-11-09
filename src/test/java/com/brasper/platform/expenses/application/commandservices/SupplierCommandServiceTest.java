package com.brasper.platform.expenses.application.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brasper.platform.expenses.domain.model.aggregates.Supplier;
import com.brasper.platform.expenses.domain.model.commands.CreateSupplierCommand;
import com.brasper.platform.expenses.domain.model.commands.UpdateSupplierCommand;
import com.brasper.platform.expenses.domain.model.valueobjects.SupplierType;
import com.brasper.platform.expenses.domain.repositories.SupplierRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Ruc;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SupplierCommandServiceTest {

    private static final Ruc RUC = new Ruc("20123456789");

    private final SupplierRepository suppliers = mock(SupplierRepository.class);
    private final SupplierCommandService service = new SupplierCommandService(suppliers);

    private static CreateSupplierCommand company() {
        return new CreateSupplierCommand("Ferreteria Sur", RUC, SupplierType.COMPANY, null);
    }

    @Test
    void createsACompany() {
        when(suppliers.existsByRuc(RUC)).thenReturn(false);
        when(suppliers.save(any(Supplier.class))).thenAnswer(call -> call.getArgument(0));

        Supplier supplier = service.handle(company()).success().orElseThrow();

        assertThat(supplier.getRuc()).isEqualTo(RUC);
        assertThat(supplier.getType()).isEqualTo(SupplierType.COMPANY);
    }

    @Test
    void createsANaturalPersonWithoutRucAndSkipsTheDuplicateCheck() {
        when(suppliers.save(any(Supplier.class))).thenAnswer(call -> call.getArgument(0));

        Supplier supplier = service.handle(
                new CreateSupplierCommand("Juan", null, SupplierType.NATURAL_PERSON, null)).success().orElseThrow();

        assertThat(supplier.getRuc()).isNull();
        verify(suppliers, never()).existsByRuc(any());
    }

    @Test
    void rejectsADuplicateRucAndACompanyWithoutRuc() {
        when(suppliers.existsByRuc(RUC)).thenReturn(true);

        assertThat(service.handle(company()).failure()).contains(SupplierCommandFailure.DUPLICATE_RUC);
        assertThat(service.handle(new CreateSupplierCommand("Ferreteria", null, SupplierType.COMPANY, null)).failure())
                .contains(SupplierCommandFailure.RUC_REQUIRED_FOR_COMPANY);
        verify(suppliers, never()).save(any());
    }

    @Test
    void updateChangesNameAndPhoneOrReportsAMissingSupplier() {
        Supplier supplier = new Supplier(company());
        when(suppliers.findById(1L)).thenReturn(Optional.of(supplier));
        when(suppliers.findById(9L)).thenReturn(Optional.empty());
        when(suppliers.save(supplier)).thenReturn(supplier);

        Supplier updated = service.handle(new UpdateSupplierCommand(1L, "Ferreteria Norte", "999"))
                .success().orElseThrow();

        assertThat(updated.getBusinessName()).isEqualTo("Ferreteria Norte");
        assertThat(updated.getRuc()).isEqualTo(RUC);
        assertThat(service.handle(new UpdateSupplierCommand(9L, "X", null)).failure())
                .contains(SupplierCommandFailure.SUPPLIER_NOT_FOUND);
    }
}
