package com.brasper.platform.treasury.infrastructure.persistence.jpa;

import com.brasper.platform.treasury.domain.model.aggregates.Partner;
import com.brasper.platform.treasury.domain.repositories.PartnerRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerJpaRepository extends JpaRepository<Partner, Long>, PartnerRepository {
}
