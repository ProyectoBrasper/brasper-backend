package com.brasper.platform.billing.infrastructure.persistence.jpa;

import com.brasper.platform.billing.domain.model.aggregates.Invoice;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceNumber;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceStatus;
import com.brasper.platform.billing.domain.repositories.InvoiceRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceJpaRepository extends JpaRepository<Invoice, Long>, InvoiceRepository {

    String FILTERS = """
            where (:status is null or i.status = :status) and (:projectId is null or i.projectId = :projectId)
              and i.issuedOn between :from and :to""";

    boolean existsBySeriesAndCorrelative(String series, int correlative);

    /** The number is stored as two columns, so the derived query takes them apart. */
    @Override
    default boolean existsByNumber(InvoiceNumber number) {
        return existsBySeriesAndCorrelative(number.series(), number.correlative());
    }

    @Query("select i from Invoice i " + FILTERS + " order by i.issuedOn desc, i.id desc")
    List<Invoice> filterPage(@Param("status") InvoiceStatus status, @Param("projectId") Long projectId,
            @Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);

    @Override
    default List<Invoice> filter(InvoiceStatus status, Long projectId, LocalDate from, LocalDate to, int page,
            int size) {
        return filterPage(status, projectId, from, to, PageRequest.of(page, size));
    }

    @Override
    @Query("select count(i) from Invoice i " + FILTERS)
    long countFiltered(@Param("status") InvoiceStatus status, @Param("projectId") Long projectId,
            @Param("from") LocalDate from, @Param("to") LocalDate to);
}
