package com.brasper.platform.projects.infrastructure.persistence.jpa;

import com.brasper.platform.projects.domain.model.aggregates.Client;
import com.brasper.platform.projects.domain.repositories.ClientRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientJpaRepository extends JpaRepository<Client, Long>, ClientRepository {
}
