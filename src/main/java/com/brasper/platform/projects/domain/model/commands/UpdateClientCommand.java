package com.brasper.platform.projects.domain.model.commands;

/** The RUC identifies the client and cannot be changed. */
public record UpdateClientCommand(Long clientId, String businessName, String contactName, String phone,
        String email) {
}
