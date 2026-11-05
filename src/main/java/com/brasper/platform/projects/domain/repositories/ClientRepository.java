package com.brasper.platform.projects.domain.repositories;

import com.brasper.platform.projects.domain.model.aggregates.Client;
import com.brasper.platform.shared.domain.model.valueobjects.Ruc;
import java.util.List;
import java.util.Optional;

/** Port used by the application layer. Implemented by Spring Data in {@code infrastructure}. */
public interface ClientRepository {

    Client save(Client client);

    Optional<Client> findById(Long id);

    boolean existsByRuc(Ruc ruc);

    List<Client> findAll();
}
