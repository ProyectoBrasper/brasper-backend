package com.brasper.platform.treasury.domain.repositories;

import com.brasper.platform.treasury.domain.model.aggregates.Partner;
import java.util.List;
import java.util.Optional;

public interface PartnerRepository {

    Partner save(Partner partner);

    Optional<Partner> findById(Long id);

    boolean existsByFullName(String fullName);

    List<Partner> findAll();
}
