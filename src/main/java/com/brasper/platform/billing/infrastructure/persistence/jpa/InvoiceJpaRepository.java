package com.brasper.platform.billing.infrastructure.persistence.jpa;

import com.brasper.platform.billing.domain.model.aggregates.Invoice;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceNumber;
import com.brasper.platform.billing.domain.repositories.InvoiceRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceJpaRepository extends JpaRepository<Invoice, Long>, InvoiceRepository {

    boolean existsBySeriesAndCorrelative(String series, int correlative);

    /** The number is stored as two columns, so the derived query takes them apart. */
    @Override
    default boolean existsByNumber(InvoiceNumber number) {
        return existsBySeriesAndCorrelative(number.series(), number.correlative());
    }
}
