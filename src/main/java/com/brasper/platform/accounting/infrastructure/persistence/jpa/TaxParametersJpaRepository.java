package com.brasper.platform.accounting.infrastructure.persistence.jpa;

import com.brasper.platform.accounting.domain.model.aggregates.TaxParameters;
import com.brasper.platform.accounting.domain.repositories.TaxParametersRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxParametersJpaRepository extends JpaRepository<TaxParameters, Long>, TaxParametersRepository {}
