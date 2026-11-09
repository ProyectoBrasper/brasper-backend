package com.brasper.platform.projects.application.queryservices;

import com.brasper.platform.projects.domain.model.aggregates.Client;
import com.brasper.platform.projects.domain.repositories.ClientRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClientQueryService {

    private final ClientRepository clients;

    public ClientQueryService(ClientRepository clients) {
        this.clients = clients;
    }

    public List<Client> findAll() {
        return clients.findAll();
    }

    public Optional<Client> findById(Long id) {
        return clients.findById(id);
    }
}
