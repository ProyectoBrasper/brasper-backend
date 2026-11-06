package com.brasper.platform.treasury.application.queryservices;

import com.brasper.platform.treasury.domain.model.aggregates.Partner;
import com.brasper.platform.treasury.domain.repositories.PartnerRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PartnerQueryService {

    private final PartnerRepository partners;

    public PartnerQueryService(PartnerRepository partners) {
        this.partners = partners;
    }

    public List<Partner> findAll() {
        return partners.findAll();
    }

    public Optional<Partner> findById(Long id) {
        return partners.findById(id);
    }
}
