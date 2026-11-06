package com.brasper.platform.staff.infrastructure.persistence.jpa;

import com.brasper.platform.staff.domain.model.aggregates.FixedCost;
import com.brasper.platform.staff.domain.repositories.FixedCostRepository;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FixedCostJpaRepository extends JpaRepository<FixedCost, Long>, FixedCostRepository {}
