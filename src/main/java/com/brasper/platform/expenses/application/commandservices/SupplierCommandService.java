package com.brasper.platform.expenses.application.commandservices;

import com.brasper.platform.expenses.domain.model.aggregates.Supplier;
import com.brasper.platform.expenses.domain.model.commands.CreateSupplierCommand;
import com.brasper.platform.expenses.domain.model.commands.UpdateSupplierCommand;
import com.brasper.platform.expenses.domain.model.valueobjects.SupplierType;
import com.brasper.platform.expenses.domain.repositories.SupplierRepository;
import com.brasper.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierCommandService {

    private final SupplierRepository suppliers;

    public SupplierCommandService(SupplierRepository suppliers) {
        this.suppliers = suppliers;
    }

    @Transactional
    public Result<Supplier, SupplierCommandFailure> handle(CreateSupplierCommand command) {
        if (command.type() == SupplierType.COMPANY && command.ruc() == null) {
            return Result.failure(SupplierCommandFailure.RUC_REQUIRED_FOR_COMPANY);
        }
        if (command.ruc() != null && suppliers.existsByRuc(command.ruc())) {
            return Result.failure(SupplierCommandFailure.DUPLICATE_RUC);
        }
        return Result.success(suppliers.save(new Supplier(command)));
    }

    @Transactional
    public Result<Supplier, SupplierCommandFailure> handle(UpdateSupplierCommand command) {
        Supplier supplier = suppliers.findById(command.supplierId()).orElse(null);
        if (supplier == null) {
            return Result.failure(SupplierCommandFailure.SUPPLIER_NOT_FOUND);
        }
        supplier.update(command);
        return Result.success(suppliers.save(supplier));
    }
}
