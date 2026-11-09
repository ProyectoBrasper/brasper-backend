package com.brasper.platform.billing.domain.repositories;

import com.brasper.platform.billing.domain.model.aggregates.Invoice;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceNumber;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Port used by the application layer. Implemented by Spring Data in {@code infrastructure}. */
public interface InvoiceRepository {

    Invoice save(Invoice invoice);

    Optional<Invoice> findById(Long id);

    boolean existsByNumber(InvoiceNumber number);

    /** Newest first. {@code status} and {@code projectId} are optional (null); {@code from} and {@code to} bound the issue date, both inclusive and never null. */
    List<Invoice> filter(InvoiceStatus status, Long projectId, LocalDate from, LocalDate to, int page, int size);

    long countFiltered(InvoiceStatus status, Long projectId, LocalDate from, LocalDate to);

    /** Money received from {@code from} to {@code to} (both inclusive): deposits only, voided invoices left out. */
    BigDecimal sumDepositsReceived(LocalDate from, LocalDate to);

    /** IGV of the invoices issued from {@code from} to {@code to} (both inclusive), voided ones left out. */
    BigDecimal sumIgvIssued(LocalDate from, LocalDate to);
}
