package com.brasper.platform.treasury.infrastructure.persistence.jpa;

import com.brasper.platform.treasury.domain.model.aggregates.Payable;
import com.brasper.platform.treasury.domain.repositories.PayableRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PayableJpaRepository extends JpaRepository<Payable, Long>, PayableRepository {

    @Override
    @Query("select distinct p from Payable p left join fetch p.payments")
    List<Payable> findAllWithPayments();
}
