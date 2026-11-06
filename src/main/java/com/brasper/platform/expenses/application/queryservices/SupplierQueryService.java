package com.brasper.platform.expenses.application.queryservices;

import com.brasper.platform.expenses.domain.model.aggregates.Supplier;
import com.brasper.platform.expenses.domain.repositories.SupplierRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SupplierQueryService {

    private final SupplierRepository suppliers;

    public SupplierQueryService(SupplierRepository suppliers) {
        this.suppliers = suppliers;
    }

    public List<Supplier> findAll() {
        return suppliers.findAll();
    }

    public Optional<Supplier> findById(Long id) {
        return suppliers.findById(id);
    }
}
