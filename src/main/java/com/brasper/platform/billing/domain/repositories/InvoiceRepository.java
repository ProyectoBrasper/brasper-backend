package com.brasper.platform.billing.domain.repositories;

import com.brasper.platform.billing.domain.model.aggregates.Invoice;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceNumber;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Port used by the application layer. Implemented by Spring Data in {@code infrastructure}. */
public interface InvoiceRepository {

    Invoice save(Invoice invoice);

    Optional<Invoice> findById(Long id);

    boolean existsByNumber(InvoiceNumber number);

    List<Invoice> findAll();

    /** Newest first. {@code status} and {@code projectId} are optional (null); {@code from} and {@code to} bound the issue date, both inclusive and never null. */
    List<Invoice> filter(InvoiceStatus status, Long projectId, LocalDate from, LocalDate to, int page, int size);

    long countFiltered(InvoiceStatus status, Long projectId, LocalDate from, LocalDate to);
}
