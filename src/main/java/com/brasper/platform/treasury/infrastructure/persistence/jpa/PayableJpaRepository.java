package com.brasper.platform.treasury.infrastructure.persistence.jpa;

import com.brasper.platform.shared.domain.model.valueobjects.PaymentStatus;
import com.brasper.platform.treasury.domain.model.aggregates.Payable;
import com.brasper.platform.treasury.domain.repositories.PayableRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayableJpaRepository extends JpaRepository<Payable, Long>, PayableRepository {

    @Override
    @Query("select distinct p from Payable p left join fetch p.payments")
    List<Payable> findAllWithPayments();

    @Query("select p from Payable p where (:status is null or p.status = :status) order by p.id")
    List<Payable> filterPage(@Param("status") PaymentStatus status, Pageable pageable);

    @Override
    default List<Payable> filter(PaymentStatus status, int page, int size) {
        return filterPage(status, PageRequest.of(page, size));
    }

    @Override
    @Query("select count(p) from Payable p where (:status is null or p.status = :status)")
    long countFiltered(@Param("status") PaymentStatus status);
}
