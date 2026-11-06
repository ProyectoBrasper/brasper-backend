package com.brasper.platform.treasury.infrastructure.persistence.jpa;

import com.brasper.platform.treasury.domain.model.aggregates.Payable;
import com.brasper.platform.treasury.domain.repositories.PayableRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayableJpaRepository extends JpaRepository<Payable, Long>, PayableRepository {
}
