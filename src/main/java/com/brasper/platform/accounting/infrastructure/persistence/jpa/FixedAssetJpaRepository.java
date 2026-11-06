package com.brasper.platform.accounting.infrastructure.persistence.jpa;

import com.brasper.platform.accounting.domain.model.aggregates.FixedAsset;
import com.brasper.platform.accounting.domain.repositories.FixedAssetRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FixedAssetJpaRepository extends JpaRepository<FixedAsset, Long>, FixedAssetRepository { }
