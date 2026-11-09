package com.brasper.platform.projects.application.commandservices;

import com.brasper.platform.projects.domain.model.aggregates.Client;
import com.brasper.platform.projects.domain.model.commands.CreateClientCommand;
import com.brasper.platform.projects.domain.model.commands.UpdateClientCommand;
import com.brasper.platform.projects.domain.repositories.ClientRepository;
import com.brasper.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientCommandService {

    private final ClientRepository clients;

    public ClientCommandService(ClientRepository clients) {
        this.clients = clients;
    }

    @Transactional
    public Result<Client, ClientCommandFailure> handle(CreateClientCommand command) {
        if (clients.existsByRuc(command.ruc())) {
            return Result.failure(ClientCommandFailure.DUPLICATE_RUC);
        }
        return Result.success(clients.save(new Client(command)));
    }

    @Transactional
    public Result<Client, ClientCommandFailure> handle(UpdateClientCommand command) {
        Client client = clients.findById(command.clientId()).orElse(null);
        if (client == null) {
            return Result.failure(ClientCommandFailure.CLIENT_NOT_FOUND);
        }
        client.update(command);
        return Result.success(clients.save(client));
    }
}
