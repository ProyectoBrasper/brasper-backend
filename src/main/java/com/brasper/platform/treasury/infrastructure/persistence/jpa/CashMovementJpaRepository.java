package com.brasper.platform.treasury.infrastructure.persistence.jpa;

import com.brasper.platform.treasury.domain.model.entities.CashMovement;
import com.brasper.platform.treasury.domain.repositories.CashMovementRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashMovementJpaRepository extends JpaRepository<CashMovement, Long>, CashMovementRepository {
}
