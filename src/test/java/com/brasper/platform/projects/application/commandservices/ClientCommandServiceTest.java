package com.brasper.platform.projects.application.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brasper.platform.projects.domain.model.aggregates.Client;
import com.brasper.platform.projects.domain.model.commands.CreateClientCommand;
import com.brasper.platform.projects.domain.model.commands.UpdateClientCommand;
import com.brasper.platform.projects.domain.repositories.ClientRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Ruc;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ClientCommandServiceTest {

    private static final Ruc RUC = new Ruc("20123456789");

    private final ClientRepository clients = mock(ClientRepository.class);
    private final ClientCommandService service = new ClientCommandService(clients);

    private static CreateClientCommand create() {
        return new CreateClientCommand("  Linings SAC ", RUC, " ", null, "ventas@linings.pe");
    }

    @Test
    void createStoresTheClientWithBlankFieldsAsNull() {
        when(clients.existsByRuc(RUC)).thenReturn(false);
        when(clients.save(any(Client.class))).thenAnswer(call -> call.getArgument(0));

        Client client = service.handle(create()).success().orElseThrow();

        assertThat(client.getBusinessName()).isEqualTo("Linings SAC");
        assertThat(client.getContactName()).isNull();
        assertThat(client.getRuc()).isEqualTo(RUC);
    }

    @Test
    void createRejectsADuplicateRuc() {
        when(clients.existsByRuc(RUC)).thenReturn(true);

        assertThat(service.handle(create()).failure()).contains(ClientCommandFailure.DUPLICATE_RUC);
        verify(clients, never()).save(any());
    }

    @Test
    void updateChangesTheDataButNotTheRuc() {
        Client client = new Client(create());
        when(clients.findById(1L)).thenReturn(Optional.of(client));
        when(clients.save(client)).thenReturn(client);

        Client updated = service.handle(new UpdateClientCommand(1L, "Linings Perú", "Ana", "999", null))
                .success().orElseThrow();

        assertThat(updated.getBusinessName()).isEqualTo("Linings Perú");
        assertThat(updated.getContactName()).isEqualTo("Ana");
        assertThat(updated.getRuc()).isEqualTo(RUC);
    }

    @Test
    void updateOfAnUnknownClientFails() {
        when(clients.findById(9L)).thenReturn(Optional.empty());

        assertThat(service.handle(new UpdateClientCommand(9L, "X", null, null, null)).failure())
                .contains(ClientCommandFailure.CLIENT_NOT_FOUND);
    }
}
