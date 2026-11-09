package com.brasper.platform.accounting.domain.repositories;

import com.brasper.platform.accounting.domain.model.aggregates.FixedAsset;
import java.util.List;
import java.util.Optional;

public interface FixedAssetRepository {
    FixedAsset save(FixedAsset fixedAsset);
    Optional<FixedAsset> findById(Long id);
    List<FixedAsset> findAll();
}
