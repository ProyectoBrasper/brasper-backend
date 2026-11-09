package com.brasper.platform.projects.interfaces.rest.transform;

import com.brasper.platform.projects.domain.model.aggregates.Client;
import com.brasper.platform.projects.interfaces.rest.resources.ClientResource;

public final class ClientResourceFromEntityAssembler {

    private ClientResourceFromEntityAssembler() {
    }

    public static ClientResource toResourceFromEntity(Client client) {
        return new ClientResource(client.getId(), client.getBusinessName(), client.getRuc().value(),
                client.getContactName(), client.getPhone(), client.getEmail());
    }
}
