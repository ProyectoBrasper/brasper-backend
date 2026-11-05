package com.brasper.platform.billing.domain.repositories;

import com.brasper.platform.billing.domain.model.aggregates.Invoice;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceNumber;
import java.util.List;
import java.util.Optional;

/** Port used by the application layer. Implemented by Spring Data in {@code infrastructure}. */
public interface InvoiceRepository {

    Invoice save(Invoice invoice);

    Optional<Invoice> findById(Long id);

    boolean existsByNumber(InvoiceNumber number);

    List<Invoice> findAll();
}
