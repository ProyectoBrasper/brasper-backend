package com.brasper.platform.accounting.application.queryservices;
import com.brasper.platform.accounting.domain.model.aggregates.FixedAsset;
import com.brasper.platform.accounting.domain.repositories.FixedAssetRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FixedAssetQueryService {
    private final FixedAssetRepository assets;
    public FixedAssetQueryService(FixedAssetRepository assets) { this.assets = assets; }
    public List<FixedAsset> findAll() { return assets.findAll(); }
    public Optional<FixedAsset> findById(Long id) { return assets.findById(id); }
}
